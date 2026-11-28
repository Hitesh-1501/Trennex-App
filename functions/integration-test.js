process.env.FIRESTORE_EMULATOR_HOST ||= '127.0.0.1:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST ||= '127.0.0.1:9099';
const functionsHost = process.env.TRENNEX_FUNCTIONS_EMULATOR_HOST || '127.0.0.1:5001';

const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, Timestamp, FieldValue } = require('firebase-admin/firestore');
const { applyRoleChange } = require('./admin-roles');

initializeApp({ projectId: 'demo-trennex' });
const auth = getAuth();
const db = getFirestore();

async function main() {
  const email = `checkout-test-${Date.now()}@example.test`;
  const password = 'test-password-123';
  const user = await auth.createUser({ email, password });

  const productRef = db.doc('products/test-product');
  const variantRef = productRef.collection('variants').doc('test-variant');
  await productRef.set({
    status: 'ACTIVE', legacyId: 999, name: 'Test Product',
    thumbnail: { url: 'https://example.test/product.png' },
  });
  await variantRef.set({
    active: true,
    sku: 'TEST-SKU',
    name: 'Standard',
    priceMinor: 1000,
    mrpMinor: 1200,
    attributes: {},
  });
  await db.doc('inventory/test-variant').set({
    onHand: 5,
    available: 5,
    reserved: 0,
  });
  const cartRef = db.doc(`users/${user.uid}/cartItems/test-variant`);
  await cartRef.set({ productId: 'test-product', variantId: 'test-variant', quantity: 4 });

  const requestRef = db.doc(`users/${user.uid}/orderRequests/test-request`);
  await requestRef.set({
    schemaVersion: 1,
    status: 'PENDING',
    items: [{ legacyId: 999, quantity: 2 }],
    itemCount: 2,
    shippingAddress: { address: 'Test Street' },
    recipient: { name: 'Test User', phone: '+910000000000' },
    payment: { method: 'COD' },
    createdAt: Timestamp.now(),
  });

  const tokenResponse = await fetch(
    `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-trennex`,
    {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ email, password, returnSecureToken: true }),
    },
  );
  const tokenBody = await tokenResponse.json();
  if (!tokenResponse.ok) throw new Error(`Auth emulator sign-in failed: ${JSON.stringify(tokenBody)}`);

  const callCreateOrder = async (requestId, idToken = tokenBody.idToken) => {
    const response = await fetch(
      `http://${functionsHost}/demo-trennex/asia-south1/createOrder`,
      {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
          ...(idToken ? { Authorization: `Bearer ${idToken}` } : {}),
        },
        body: JSON.stringify({ data: { requestId } }),
      },
    );
    return response.json();
  };

  const functionBody = await callCreateOrder('test-request');
  if (!functionBody.result?.orderId) {
    throw new Error(`createOrder failed: ${JSON.stringify(functionBody)}`);
  }

  const order = await db.doc(`orders/${functionBody.result.orderId}`).get();
  const inventory = await db.doc('inventory/test-variant').get();
  const request = await requestRef.get();
  if (order.data().totalMinor !== 2000) throw new Error('Unexpected order total');
  if (order.data().preview.imageUrl !== 'https://example.test/product.png') {
    throw new Error('Order preview image was not mapped from the catalog thumbnail');
  }
  if (inventory.data().available !== 3) throw new Error('Inventory was not decremented');
  if (request.data().status !== 'PROCESSED') throw new Error('Request was not marked processed');

  const duplicate = await callCreateOrder('test-request');
  if (duplicate.error?.status !== 'FAILED_PRECONDITION') {
    throw new Error(`Duplicate request was not rejected: ${JSON.stringify(duplicate)}`);
  }

  const invalidRequestRef = db.doc(`users/${user.uid}/orderRequests/invalid-request`);
  await invalidRequestRef.set({
    schemaVersion: 1,
    status: 'PENDING',
    items: [{ legacyId: 999, quantity: 0 }],
    itemCount: 0,
    shippingAddress: { address: 'Test Street' },
    recipient: { name: 'Test User' },
    payment: { method: 'COD' },
    createdAt: Timestamp.now(),
  });
  const invalid = await callCreateOrder('invalid-request');
  if (invalid.error?.status !== 'INVALID_ARGUMENT') {
    throw new Error(`Invalid quantity was not rejected: ${JSON.stringify(invalid)}`);
  }

  const stockRequestRef = db.doc(`users/${user.uid}/orderRequests/stock-request`);
  await stockRequestRef.set({
    schemaVersion: 1,
    status: 'PENDING',
    items: [{ legacyId: 999, quantity: 4 }],
    itemCount: 4,
    shippingAddress: { address: 'Test Street' },
    recipient: { name: 'Test User' },
    payment: { method: 'COD' },
    createdAt: Timestamp.now(),
  });
  const stock = await callCreateOrder('stock-request');
  if (stock.error?.status !== 'FAILED_PRECONDITION') {
    throw new Error(`Out-of-stock request was not rejected: ${JSON.stringify(stock)}`);
  }
  const cartAfterRejection = await cartRef.get();
  if (cartAfterRejection.data()?.quantity !== 4) {
    throw new Error('Rejected checkout changed the cart');
  }

  const unauthenticated = await callCreateOrder('test-request', null);
  if (unauthenticated.error?.status !== 'UNAUTHENTICATED') {
    throw new Error(`Unauthenticated request was not rejected: ${JSON.stringify(unauthenticated)}`);
  }

  const callAssistant = async (idToken, data = { question: 'unmatchedcatalogterm' }) => {
    const response = await fetch(
      `http://${functionsHost}/demo-trennex/asia-south1/askShoppingAssistant`,
      { method: 'POST', headers: { 'content-type': 'application/json',
        ...(idToken ? { Authorization: `Bearer ${idToken}` } : {}) },
      body: JSON.stringify({ data }) },
    );
    return response.json();
  };
  const unauthenticatedAssistant = await callAssistant(null);
  if (unauthenticatedAssistant.error?.status !== 'UNAUTHENTICATED') {
    throw new Error('Unauthenticated assistant request was accepted');
  }
  const malformedAssistant = await callAssistant(tokenBody.idToken,
    { question: 'unmatchedcatalogterm', unexpected: true });
  if (malformedAssistant.error?.status !== 'INVALID_ARGUMENT') {
    throw new Error('Malformed assistant request was accepted');
  }
  for (let attempt = 0; attempt < 12; attempt += 1) {
    const noMatch = await callAssistant(tokenBody.idToken);
    if (!noMatch.result || noMatch.result.remainingToday !== 11 - attempt) {
      throw new Error(`No-match assistant request did not count toward quota: ${JSON.stringify(noMatch)}`);
    }
  }
  const overQuota = await callAssistant(tokenBody.idToken);
  if (overQuota.error?.status !== 'RESOURCE_EXHAUSTED') {
    throw new Error(`Assistant quota was bypassed: ${JSON.stringify(overQuota)}`);
  }

  const tamperedRequestRef = db.doc(`users/${user.uid}/orderRequests/tampered-request`);
  await tamperedRequestRef.set({
    schemaVersion: 1,
    status: 'PENDING',
    items: [{ legacyId: 999, quantity: 1, priceMinor: 1 }],
    itemCount: 1,
    shippingAddress: { address: 'Test Street' },
    recipient: { name: 'Test User' },
    payment: { method: 'COD' },
    createdAt: Timestamp.now(),
  });
  const tampered = await callCreateOrder('tampered-request');
  if (tampered.error?.status !== 'INVALID_ARGUMENT') {
    throw new Error(`Client price field was accepted: ${JSON.stringify(tampered)}`);
  }

  for (const badId of ['../orders/forged', '', 123, 'x'.repeat(129)]) {
    const malformed = await callCreateOrder(badId);
    if (malformed.error?.status !== 'INVALID_ARGUMENT') {
      throw new Error(`Malformed request ID was accepted: ${JSON.stringify(malformed)}`);
    }
  }
  const malformedItemRef = db.doc(`users/${user.uid}/orderRequests/malformed-item`);
  await malformedItemRef.set({ status: 'PENDING', items: [null], payment: { method: 'COD' },
    shippingAddress: { address: 'Test Street' }, recipient: { name: 'Test User' } });
  const malformedItem = await callCreateOrder('malformed-item');
  if (malformedItem.error?.status !== 'INVALID_ARGUMENT') {
    throw new Error(`Malformed checkout item was accepted: ${JSON.stringify(malformedItem)}`);
  }

  const unsupportedPaymentRef = db.doc(`users/${user.uid}/orderRequests/unsupported-payment`);
  await unsupportedPaymentRef.set({
    status: 'PENDING',
    items: [{ legacyId: 999, quantity: 1 }],
    shippingAddress: { address: 'Test Street' },
    recipient: { name: 'Test User' },
    payment: { method: 'UPI' },
  });
  const unsupportedPayment = await callCreateOrder('unsupported-payment');
  if (unsupportedPayment.error?.status !== 'INVALID_ARGUMENT') {
    throw new Error(`Unsupported payment was accepted: ${JSON.stringify(unsupportedPayment)}`);
  }

  const duplicateVariantRef = db.doc(`users/${user.uid}/orderRequests/duplicate-variant`);
  await duplicateVariantRef.set({
    status: 'PENDING',
    items: [{ legacyId: 999, quantity: 2 }, { legacyId: 999, quantity: 2 }],
    shippingAddress: { address: 'Test Street' },
    recipient: { name: 'Test User' },
    payment: { method: 'COD' },
  });
  const duplicateVariant = await callCreateOrder('duplicate-variant');
  if (duplicateVariant.error?.status !== 'FAILED_PRECONDITION') {
    throw new Error(`Duplicate variants exceeded stock: ${JSON.stringify(duplicateVariant)}`);
  }

  const roleUser = await auth.createUser({
    email: `role-test-${Date.now()}@example.test`, password,
  });
  const roleServices = { auth, db, serverTimestamp: () => FieldValue.serverTimestamp() };
  const grantOperationId = `grant-${roleUser.uid}`;
  const revokeOperationId = `revoke-${roleUser.uid}`;
  await applyRoleChange({ ...roleServices,
    command: { uid: roleUser.uid, action: 'grant', scopes: ['catalog'],
      operationId: grantOperationId, expectedVersion: 0 } });
  const repeatedGrant = await applyRoleChange({ ...roleServices,
    command: { uid: roleUser.uid, action: 'grant', scopes: ['catalog'],
      operationId: grantOperationId, expectedVersion: 0 } });
  const grantedUser = await auth.getUser(roleUser.uid);
  const grantedDoc = await db.doc(`adminRoleGrants/${roleUser.uid}`).get();
  const grantAudit = await db.doc(`adminAuditLogs/${grantOperationId}`).get();
  if (grantedUser.customClaims?.trennexRole !== 'admin'
      || grantedDoc.data()?.active !== true
      || grantedDoc.data()?.operationState !== 'SUCCEEDED'
      || grantedDoc.data()?.version !== 1
      || grantedDoc.data()?.scopes?.[0] !== 'catalog'
      || grantAudit.data()?.state !== 'SUCCEEDED'
      || repeatedGrant !== 'already-complete') {
    throw new Error('Admin SDK grant did not persist claim and scoped grant');
  }
  await applyRoleChange({ ...roleServices,
    command: { uid: roleUser.uid, action: 'revoke', scopes: [],
      operationId: revokeOperationId } });
  const revokedUser = await auth.getUser(roleUser.uid);
  const revokedDoc = await db.doc(`adminRoleGrants/${roleUser.uid}`).get();
  const revokeAudit = await db.doc(`adminAuditLogs/${revokeOperationId}`).get();
  if (revokedUser.customClaims?.trennexRole || revokedDoc.data()?.active !== false
      || revokedDoc.data()?.operationState !== 'SUCCEEDED'
      || revokedDoc.data()?.version !== 2
      || revokeAudit.data()?.state !== 'SUCCEEDED') {
    throw new Error('Admin SDK revoke did not clear role and deactivate grant');
  }

  console.log('PASS checkout, assistant quota, and Admin SDK role lifecycle cases');
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

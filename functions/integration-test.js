process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';

const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');

initializeApp({ projectId: 'demo-trennex' });
const auth = getAuth();
const db = getFirestore();

async function main() {
  const email = `checkout-test-${Date.now()}@example.test`;
  const password = 'test-password-123';
  const user = await auth.createUser({ email, password });

  const productRef = db.doc('products/test-product');
  const variantRef = productRef.collection('variants').doc('test-variant');
  await productRef.set({ status: 'ACTIVE', legacyId: 999, name: 'Test Product' });
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
    'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-trennex',
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
      'http://127.0.0.1:5001/demo-trennex/asia-south1/createOrder',
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
    shippingAddress: {},
    recipient: {},
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
    shippingAddress: {},
    recipient: {},
    payment: { method: 'COD' },
    createdAt: Timestamp.now(),
  });
  const stock = await callCreateOrder('stock-request');
  if (stock.error?.status !== 'FAILED_PRECONDITION') {
    throw new Error(`Out-of-stock request was not rejected: ${JSON.stringify(stock)}`);
  }

  const unauthenticated = await callCreateOrder('test-request', null);
  if (unauthenticated.error?.status !== 'UNAUTHENTICATED') {
    throw new Error(`Unauthenticated request was not rejected: ${JSON.stringify(unauthenticated)}`);
  }

  const tamperedRequestRef = db.doc(`users/${user.uid}/orderRequests/tampered-request`);
  await tamperedRequestRef.set({
    schemaVersion: 1,
    status: 'PENDING',
    items: [{ legacyId: 999, quantity: 1, priceMinor: 1 }],
    itemCount: 1,
    shippingAddress: {},
    recipient: {},
    payment: { method: 'COD' },
    createdAt: Timestamp.now(),
  });
  const tampered = await callCreateOrder('tampered-request');
  if (!tampered.result?.orderId) throw new Error(`Tampered request failed: ${JSON.stringify(tampered)}`);
  const tamperedOrder = await db.doc(`orders/${tampered.result.orderId}`).get();
  if (tamperedOrder.data().totalMinor !== 1000) throw new Error('Client price was trusted');

  console.log(`PASS createOrder success and rejection cases: ${functionBody.result.orderNumber}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

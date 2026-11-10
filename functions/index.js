const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { initializeApp } = require('firebase-admin/app');

initializeApp();

const db = getFirestore();
const REGION = 'asia-south1';
const MAX_QUANTITY = 10;

exports.createOrder = onCall({ region: REGION }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Sign in before placing an order.');
  }

  const requestId = request.data?.requestId;
  if (typeof requestId !== 'string' || requestId.trim().length === 0) {
    throw new HttpsError('invalid-argument', 'A checkout request ID is required.');
  }

  const uid = request.auth.uid;
  const requestRef = db.doc(`users/${uid}/orderRequests/${requestId}`);

  return db.runTransaction(async (transaction) => {
    const requestSnapshot = await transaction.get(requestRef);
    if (!requestSnapshot.exists) {
      throw new HttpsError('not-found', 'Checkout request was not found.');
    }

    const checkout = requestSnapshot.data();
    if (checkout.status !== 'PENDING') {
      throw new HttpsError('failed-precondition', 'This checkout request was already processed.');
    }

    const rawItems = Array.isArray(checkout.items) ? checkout.items : [];
    if (rawItems.length === 0 || rawItems.length > 50) {
      throw new HttpsError('invalid-argument', 'Checkout must contain between one and fifty items.');
    }

    const resolvedItems = [];
    for (const rawItem of rawItems) {
      const quantity = Number(rawItem.quantity);
      if (!Number.isInteger(quantity) || quantity < 1 || quantity > MAX_QUANTITY) {
        throw new HttpsError('invalid-argument', 'Each item quantity must be between one and ten.');
      }

      const productSnapshot = rawItem.productId
        ? await transaction.get(db.doc(`products/${rawItem.productId}`))
        : (await transaction.get(
            db.collection('products').where('legacyId', '==', Number(rawItem.legacyId)).limit(1),
          )).docs[0];

      if (!productSnapshot || !productSnapshot.exists) {
        throw new HttpsError('failed-precondition', 'One or more products are no longer available.');
      }
      const product = productSnapshot.data();
      if (product.status !== 'ACTIVE') {
        throw new HttpsError('failed-precondition', 'One or more products are no longer available.');
      }

      const variantSnapshot = rawItem.variantId
        ? await transaction.get(productSnapshot.ref.collection('variants').doc(rawItem.variantId))
        : (await transaction.get(
            productSnapshot.ref.collection('variants').where('active', '==', true).limit(1),
          )).docs[0];
      if (!variantSnapshot || !variantSnapshot.exists || variantSnapshot.data().active !== true) {
        throw new HttpsError('failed-precondition', 'A selected product variant is no longer available.');
      }

      const variant = variantSnapshot.data();
      const priceMinor = Number(variant.priceMinor);
      const mrpMinor = Number(variant.mrpMinor);
      if (!Number.isInteger(priceMinor) || priceMinor < 0 || !Number.isInteger(mrpMinor)) {
        throw new HttpsError('failed-precondition', 'A product has invalid pricing.');
      }

      const inventoryRef = db.doc(`inventory/${variantSnapshot.id}`);
      const inventorySnapshot = await transaction.get(inventoryRef);
      const inventory = inventorySnapshot.data() || {};
      const available = Number(inventory.available);
      if (!inventorySnapshot.exists || !Number.isInteger(available) || available < quantity) {
        throw new HttpsError('failed-precondition', 'One or more items are out of stock.');
      }

      resolvedItems.push({
        productId: productSnapshot.id,
        variantId: variantSnapshot.id,
        sku: variant.sku || variantSnapshot.id,
        name: product.name || product.title || 'Product',
        variantName: variant.name || '',
        attributes: variant.attributes || {},
        imageUrl: product.imageUrl || product.thumbnail || null,
        quantity,
        unitPriceMinor: priceMinor,
        unitMrpMinor: mrpMinor,
        discountMinor: Math.max(0, (mrpMinor - priceMinor) * quantity),
        lineTotalMinor: priceMinor * quantity,
        returnPolicy: product.returnPolicy || null,
        inventoryRef,
        available,
      });
    }

    const subtotalMinor = resolvedItems.reduce((total, item) => total + item.lineTotalMinor, 0);
    const shippingMinor = 0;
    const discountMinor = resolvedItems.reduce((total, item) => total + item.discountMinor, 0);
    const taxMinor = 0;
    const totalMinor = subtotalMinor + shippingMinor + taxMinor;
    const orderRef = db.collection('orders').doc();
    const orderNumber = `TNX-${Date.now().toString(36).toUpperCase()}-${orderRef.id.slice(0, 6).toUpperCase()}`;
    const now = FieldValue.serverTimestamp();

    transaction.set(orderRef, {
      schemaVersion: 1,
      orderNumber,
      userId: uid,
      status: 'CONFIRMED',
      currency: 'INR',
      subtotalMinor,
      discountMinor,
      shippingMinor,
      taxMinor,
      totalMinor,
      coupon: null,
      shippingAddress: checkout.shippingAddress || {},
      recipient: checkout.recipient || {},
      payment: checkout.payment || { method: 'COD' },
      itemCount: resolvedItems.reduce((total, item) => total + item.quantity, 0),
      preview: {
        title: resolvedItems[0].name,
        imageUrl: resolvedItems[0].imageUrl,
      },
      expectedDeliveryAt: null,
      placedAt: now,
      updatedAt: now,
    });

    resolvedItems.forEach((item, index) => {
      transaction.set(orderRef.collection('items').doc(String(index + 1)), {
        productId: item.productId,
        variantId: item.variantId,
        sku: item.sku,
        name: item.name,
        variantName: item.variantName,
        attributes: item.attributes,
        imageUrl: item.imageUrl,
        quantity: item.quantity,
        unitPriceMinor: item.unitPriceMinor,
        unitMrpMinor: item.unitMrpMinor,
        discountMinor: item.discountMinor,
        lineTotalMinor: item.lineTotalMinor,
        returnPolicy: item.returnPolicy,
        reviewStatus: 'NOT_ELIGIBLE',
      });
      transaction.update(item.inventoryRef, {
        onHand: item.available - item.quantity,
        available: item.available - item.quantity,
        updatedAt: now,
      });
    });

    transaction.update(requestRef, {
      status: 'PROCESSED',
      orderId: orderRef.id,
      processedAt: now,
      updatedAt: now,
    });

    return { orderId: orderRef.id, orderNumber };
  });
});

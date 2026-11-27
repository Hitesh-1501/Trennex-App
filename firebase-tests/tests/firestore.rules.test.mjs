import { after, before, beforeEach, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
  updateDoc,
} from 'firebase/firestore';

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(currentDirectory, '..', '..');
const projectId = 'demo-trennex';
let testEnvironment;

const user = (uid) => testEnvironment.authenticatedContext(uid).firestore();
const admin = () => testEnvironment.authenticatedContext('admin-user', { admin: true }).firestore();
const publicClient = () => testEnvironment.unauthenticatedContext().firestore();

async function seed(pathName, data) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), pathName), data);
  });
}

before(async () => {
  const rules = await readFile(path.join(repositoryRoot, 'firestore.rules'), 'utf8');
  testEnvironment = await initializeTestEnvironment({
    projectId,
    firestore: { rules, host: '127.0.0.1', port: 8080 },
  });
});

beforeEach(async () => {
  await testEnvironment.clearFirestore();
});

after(async () => {
  await testEnvironment.cleanup();
});

describe('catalog rules', () => {
  test('the public can read active products but not drafts', async () => {
    await seed('products/active-shirt', { status: 'ACTIVE', title: 'Active shirt' });
    await seed('products/draft-shirt', { status: 'DRAFT', title: 'Draft shirt' });
    await seed('products/draft-active-shirt', { status: 'DRAFT', active: true, title: 'Draft' });
    await seed('products/draft-legacy-shirt', { id: 3, title: 'Draft', price: 1, status: 'DRAFT' });

    await assertSucceeds(getDoc(doc(publicClient(), 'products/active-shirt')));
    await assertFails(getDoc(doc(publicClient(), 'products/draft-shirt')));
    await assertFails(getDoc(doc(publicClient(), 'products/draft-active-shirt')));
    await assertFails(getDoc(doc(publicClient(), 'products/draft-legacy-shirt')));
  });

  test('the current unversioned product and category documents remain readable during migration', async () => {
    await seed('products/legacy-shirt', {
      id: 1,
      title: 'Legacy shirt',
      price: 1299,
    });
    await seed('categories/mens-shirts', {
      slug: 'mens-shirts',
      name: 'Mens Shirts',
      url: 'https://example.test/categories/mens-shirts',
    });

    await assertSucceeds(getDoc(doc(publicClient(), 'products/legacy-shirt')));
    await assertSucceeds(getDoc(doc(publicClient(), 'categories/mens-shirts')));
  });

  test('only an admin can write catalog documents', async () => {
    await assertFails(setDoc(doc(user('buyer-1'), 'products/new-shirt'), {
      status: 'ACTIVE',
      title: 'New shirt',
    }));
    await assertSucceeds(setDoc(doc(admin(), 'products/new-shirt'), {
      status: 'ACTIVE',
      title: 'New shirt',
    }));
  });
});

describe('user-owned data', () => {
  test('a user can update their profile but cannot read another profile', async () => {
    await assertSucceeds(setDoc(doc(user('buyer-1'), 'users/buyer-1'), {
      schemaVersion: 1,
      displayName: 'Buyer One',
      email: 'buyer@example.com',
      updatedAt: serverTimestamp(),
    }));
    await assertFails(getDoc(doc(user('buyer-2'), 'users/buyer-1')));
  });

  test('cart quantities must remain between one and ten', async () => {
    const valid = {
      schemaVersion: 1,
      productId: 'shirt-1',
      variantId: 'black-m',
      quantity: 2,
      selected: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    await assertSucceeds(setDoc(doc(user('buyer-1'), 'users/buyer-1/cartItems/shirt-black-m'), valid));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1/cartItems/invalid'), {
      ...valid,
      quantity: 11,
    }));
  });

  test('notification owners may only change readAt', async () => {
    await seed('users/buyer-1/notifications/notice-1', {
      title: 'Order shipped',
      createdAt: new Date(),
      readAt: null,
    });

    await assertSucceeds(updateDoc(doc(user('buyer-1'), 'users/buyer-1/notifications/notice-1'), {
      readAt: serverTimestamp(),
    }));
    await assertFails(updateDoc(doc(user('buyer-1'), 'users/buyer-1/notifications/notice-1'), {
      title: 'Changed by client',
    }));
  });
});

describe('order migration rules', () => {
  test('top-level orders are readable only by their owner and are not client-writable', async () => {
    await seed('orders/order-1', { userId: 'buyer-1', status: 'PLACED' });

    await assertSucceeds(getDoc(doc(user('buyer-1'), 'orders/order-1')));
    await assertFails(getDoc(doc(user('buyer-2'), 'orders/order-1')));
    await assertFails(setDoc(doc(user('buyer-1'), 'orders/order-2'), {
      userId: 'buyer-1',
      status: 'PLACED',
    }));
  });

  test('legacy orders stay readable but customer-created orders are denied', async () => {
    const legacyOrder = {
      id: 101,
      title: 'T-shirt',
      description: 'Legacy checkout item',
      imageUrl: 'https://example.test/shirt.jpg',
      imageRes: 0,
      price: 1299,
      quantity: 1,
      status: 'Pending',
      orderDate: '2026-11-08',
      expectedDeliveryDate: '2026-11-12',
    };
    await seed('users/buyer-1/orders/101', legacyOrder);
    await assertSucceeds(getDoc(doc(user('buyer-1'), 'users/buyer-1/orders/101')));
    await assertFails(getDoc(doc(user('buyer-2'), 'users/buyer-1/orders/101')));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1/orders/102'), legacyOrder));
  });

  test('users can create immutable checkout requests but cannot create orders directly', async () => {
    const request = {
      schemaVersion: 1,
      status: 'PENDING',
      items: [{ legacyId: 101, quantity: 1 }],
      itemCount: 1,
      shippingAddress: { address: '1 Main Street' },
      recipient: { name: 'Buyer One', phone: '+910000000000' },
      payment: { method: 'COD' },
      createdAt: serverTimestamp(),
    };

    await assertSucceeds(setDoc(
      doc(user('buyer-1'), 'users/buyer-1/orderRequests/request-1'),
      request,
    ));
    await assertSucceeds(getDoc(doc(
      user('buyer-1'),
      'users/buyer-1/orderRequests/request-1',
    )));
    await assertFails(getDoc(doc(
      user('buyer-2'),
      'users/buyer-1/orderRequests/request-1',
    )));
    await assertFails(updateDoc(
      doc(user('buyer-1'), 'users/buyer-1/orderRequests/request-1'),
      { status: 'CONFIRMED' },
    ));
  });
});

describe('B4 access boundaries', () => {
  test('signed-out and another account cannot read private customer data', async () => {
    await seed('users/buyer-1/savedAddresses/home', { address: 'Private Street' });
    await seed('users/buyer-1/cartItems/sku', { productId: 'shirt', variantId: 'sku', quantity: 1 });
    await seed('users/buyer-1/wishlistItems/sku', { productId: 'shirt', variantId: 'sku' });
    await seed('users/buyer-1/collections/favorites', { name: 'Favorites' });
    for (const pathName of ['savedAddresses/home', 'cartItems/sku', 'wishlistItems/sku', 'collections/favorites']) {
      await assertFails(getDoc(doc(publicClient(), `users/buyer-1/${pathName}`)));
      await assertFails(getDoc(doc(user('buyer-2'), `users/buyer-1/${pathName}`)));
      await assertSucceeds(getDoc(doc(user('buyer-1'), `users/buyer-1/${pathName}`)));
    }
  });

  test('client cannot escalate privilege or write inventory, prices, or unknown paths', async () => {
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1'), { role: 'admin' }));
    await assertFails(setDoc(doc(user('buyer-1'), 'inventory/sku'), { available: 100 }));
    await assertFails(setDoc(doc(user('buyer-1'), 'products/shirt'), { status: 'ACTIVE', priceMinor: 1 }));
    await assertFails(setDoc(doc(publicClient(), 'banners/banner'), { active: true }));
    await assertFails(setDoc(doc(user('buyer-1'), 'unexpected/data'), { value: true }));
  });

  test('profile and cart reject malformed or unexpected fields', async () => {
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1'), { name: 123 }));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1'), { name: 'Buyer', admin: true }));
    const cart = { schemaVersion: 1, productId: 'shirt', variantId: 'sku', quantity: 1,
      selected: true, createdAt: serverTimestamp(), updatedAt: serverTimestamp() };
    await assertSucceeds(setDoc(doc(user('buyer-1'), 'users/buyer-1/cartItems/sku'), cart));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1/cartItems/bad'), { ...cart, quantity: '2' }));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1/cartItems/bad'), { ...cart, quantity: 11 }));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1/cartItems/bad'), { ...cart, priceMinor: 1 }));
    await assertFails(setDoc(doc(user('buyer-2'), 'users/buyer-1/cartItems/sku'), cart));
  });

  test('profile creation time stays immutable and address coordinates are bounded', async () => {
    await assertSucceeds(setDoc(doc(user('buyer-1'), 'users/buyer-1'),
      { name: 'Buyer', createdAt: serverTimestamp() }));
    await assertFails(updateDoc(doc(user('buyer-1'), 'users/buyer-1'),
      { createdAt: serverTimestamp() }));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1/savedAddresses/bad'),
      { address: 'Street', latitude: 101 }));
    await assertSucceeds(setDoc(doc(user('buyer-1'), 'users/buyer-1/savedAddresses/good'),
      { address: 'Street', latitude: 20, longitude: 80 }));
  });

  test('checkout request rejects malformed input and cannot become an order', async () => {
    const valid = { schemaVersion: 1, status: 'PENDING', items: [{ legacyId: 101, quantity: 1 }],
      itemCount: 1, shippingAddress: { address: 'Street' }, recipient: { name: 'Buyer' },
      payment: { method: 'COD' }, createdAt: serverTimestamp() };
    await assertFails(setDoc(doc(publicClient(), 'users/buyer-1/orderRequests/a'), valid));
    await assertFails(setDoc(doc(user('buyer-2'), 'users/buyer-1/orderRequests/a'), valid));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1/orderRequests/a'), { ...valid, items: [] }));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1/orderRequests/a'), { ...valid, payment: { method: 'UPI' } }));
    await assertFails(setDoc(doc(user('buyer-1'), 'users/buyer-1/orderRequests/a'), { ...valid, totalMinor: 1 }));
    await assertSucceeds(setDoc(doc(user('buyer-1'), 'users/buyer-1/orderRequests/a'), valid));
    await assertFails(updateDoc(doc(user('buyer-1'), 'users/buyer-1/orderRequests/a'), { status: 'PROCESSED' }));
  });
});

describe('review moderation', () => {
  test('published reviews are public while pending reviews are private to their author', async () => {
    await seed('products/shirt-1/reviews/review-1', {
      userId: 'buyer-1',
      status: 'PUBLISHED',
      rating: 5,
    });
    await seed('products/shirt-1/reviews/review-2', {
      userId: 'buyer-1',
      status: 'PENDING',
      rating: 4,
    });

    await assertSucceeds(getDoc(doc(publicClient(), 'products/shirt-1/reviews/review-1')));
    await assertFails(getDoc(doc(publicClient(), 'products/shirt-1/reviews/review-2')));
    await assertSucceeds(getDoc(doc(user('buyer-1'), 'products/shirt-1/reviews/review-2')));
    assert.ok(true);
  });
});

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

    await assertSucceeds(getDoc(doc(publicClient(), 'products/active-shirt')));
    await assertFails(getDoc(doc(publicClient(), 'products/draft-shirt')));
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

  test('the current legacy pending-order write remains temporarily supported', async () => {
    await assertSucceeds(setDoc(doc(user('buyer-1'), 'users/buyer-1/orders/101'), {
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
    }));
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

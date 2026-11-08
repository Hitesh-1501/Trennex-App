import assert from 'node:assert/strict';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getStorage } from 'firebase-admin/storage';

const projectId = process.env.GCLOUD_PROJECT || 'demo-trennex';
if (projectId !== 'demo-trennex') {
  throw new Error(`Verification refused: expected demo-trennex but received ${projectId}.`);
}
if (!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_STORAGE_EMULATOR_HOST) {
  throw new Error('Verification requires both Firestore and Storage emulators.');
}

initializeApp({
  credential: applicationDefault(),
  projectId,
  storageBucket: `${projectId}.appspot.com`,
});

const database = getFirestore();
const bucket = getStorage().bucket();
const expected = {
  categories: 10,
  brands: 8,
  products: 11,
  variants: 15,
  inventory: 15,
  banners: 3,
  homeSections: 3,
  storageAssets: 24,
};

const snapshots = await Promise.all([
  database.collection('categories').get(),
  database.collection('brands').get(),
  database.collection('products').get(),
  database.collectionGroup('variants').get(),
  database.collection('inventory').get(),
  database.collection('banners').get(),
  database.collection('homeSections').get(),
]);

const actual = {
  categories: snapshots[0].size,
  brands: snapshots[1].size,
  products: snapshots[2].size,
  variants: snapshots[3].size,
  inventory: snapshots[4].size,
  banners: snapshots[5].size,
  homeSections: snapshots[6].size,
};

for (const [name, expectedCount] of Object.entries(expected)) {
  if (name !== 'storageAssets') {
    assert.equal(actual[name], expectedCount, `${name} count does not match the seed manifest`);
  }
}

for (const product of snapshots[2].docs) {
  const data = product.data();
  assert.equal(data.status, 'ACTIVE', `${product.id} is not active`);
  assert.equal(data.currency, 'INR', `${product.id} does not use INR`);
  assert.ok(Number.isInteger(data.minPriceMinor), `${product.id} price is not integer paise`);
  assert.ok(data.minPriceMinor > 0, `${product.id} price must be positive`);
  assert.ok(data.minMrpMinor >= data.minPriceMinor, `${product.id} MRP is below its price`);
  assert.ok(data.thumbnail?.storagePath?.startsWith('catalog/products/'), `${product.id} has no catalog image`);
}

const publicConfig = await database.doc('appConfig/public').get();
assert.equal(publicConfig.exists, true, 'appConfig/public is missing');
assert.equal(publicConfig.get('featureFlags.firebaseCatalog'), true, 'Firebase catalog flag is disabled');

const [files] = await bucket.getFiles({ prefix: 'catalog/' });
assert.equal(files.length, expected.storageAssets, 'Storage asset count does not match the seed manifest');

console.log('TrenNex seed snapshot verified successfully.');
console.table({ ...actual, storageAssets: files.length });

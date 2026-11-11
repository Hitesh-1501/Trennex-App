import assert from 'node:assert/strict';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getStorage } from 'firebase-admin/storage';

const projectId = process.env.GCLOUD_PROJECT;
if (projectId !== 'trennex-staging-ebbeb') throw new Error('Staging verification requires GCLOUD_PROJECT=trennex-staging-ebbeb.');
if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) throw new Error('GOOGLE_APPLICATION_CREDENTIALS is not set.');
const bucketName = process.env.STAGING_STORAGE_BUCKET || `${projectId}.firebasestorage.app`;

initializeApp({ credential: applicationDefault(), projectId, storageBucket: bucketName });
const database = getFirestore();
const bucket = getStorage().bucket();
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
const expected = { categories: 10, brands: 8, products: 11, variants: 15, inventory: 15, banners: 3, homeSections: 3 };
for (const [name, count] of Object.entries(expected)) assert.equal(actual[name], count, `${name} count mismatch`);
for (const product of snapshots[2].docs) {
  assert.equal(product.get('status'), 'ACTIVE', `${product.id} is not active`);
  assert.equal(product.get('currency'), 'INR', `${product.id} is not INR`);
}
const [files] = await bucket.getFiles({ prefix: 'catalog/' });
assert.equal(files.length, 24, 'Storage asset count mismatch');
console.log(`Staging catalog verified in ${projectId}.`);
console.table({ ...actual, storageAssets: files.length });

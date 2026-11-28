import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';
import { getStorage } from 'firebase-admin/storage';
import { validateCatalogAsset } from './catalog-image-validation.mjs';

const projectId = process.env.GCLOUD_PROJECT;
assert.equal(projectId, 'trennex-staging-ebbeb', 'Media publishing is staging-only.');
assert.ok(process.env.GOOGLE_APPLICATION_CREDENTIALS, 'Set GOOGLE_APPLICATION_CREDENTIALS.');

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const assetRoot = path.join(root, 'firebase-data', 'assets');
const catalog = JSON.parse(await readFile(path.join(root, 'firebase-data', 'catalog.seed.json'), 'utf8'));
const bucketName = process.env.STAGING_STORAGE_BUCKET || `${projectId}.firebasestorage.app`;
initializeApp({ credential: applicationDefault(), projectId, storageBucket: bucketName });
const db = getFirestore();
const bucket = getStorage().bucket();
assert.equal((await bucket.exists())[0], true, `Storage bucket ${bucketName} does not exist.`);
await Promise.all([
  ...catalog.products.map((item) => validateCatalogAsset(assetRoot, item.imageAsset)),
  ...catalog.banners.map((item) => validateCatalogAsset(assetRoot, item.imageAsset)),
]);

// Read every target before uploading. A missing or mismatched catalog stops the entire update.
const products = await Promise.all(catalog.products.map(async (item) => {
  const ref = db.doc(`products/${item.id}`);
  const snapshot = await ref.get();
  assert.equal(snapshot.exists, true, `Missing staging product ${item.id}`);
  assert.equal(snapshot.get('legacyId'), item.legacyId, `Product mismatch: ${item.id}`);
  const variants = await ref.collection('variants').get();
  assert.equal(variants.size, item.variants.length, `Variant count mismatch: ${item.id}`);
  for (const variant of item.variants) {
    assert.ok(variants.docs.some((doc) => doc.id === variant.id), `Missing variant ${variant.id}`);
  }
  return { item, ref };
}));
const banners = await Promise.all(catalog.banners.map(async (item) => {
  const ref = db.doc(`banners/${item.id}`);
  assert.equal((await ref.get()).exists, true, `Missing staging banner ${item.id}`);
  return { item, ref };
}));

function imageUrl(storagePath) {
  return `https://firebasestorage.googleapis.com/v0/b/${bucketName}/o/${encodeURIComponent(storagePath)}?alt=media`;
}

async function upload(assetName, storagePath, altText) {
  assert.ok(assetName.startsWith('products/') || assetName.startsWith('banners/'));
  const image = await validateCatalogAsset(assetRoot, assetName);
  assert.equal(image.contentType, 'image/jpeg', 'V2 publication requires approved JPEG assets.');
  await bucket.upload(image.filePath, {
    destination: storagePath,
    metadata: {
      contentType: image.contentType,
      cacheControl: 'public,max-age=3600',
      metadata: { publishedBy: 'trennex-staging-media-v2' },
    },
  });
  return { storagePath, url: imageUrl(storagePath), altText };
}

const batch = db.batch();
for (const { item, ref } of products) {
  const image = await upload(item.imageAsset, `catalog/products/${item.id}/primary-v2.jpg`, item.name);
  batch.update(ref, {
    thumbnail: image,
    images: [{ ...image, sortOrder: 1 }],
    updatedAt: FieldValue.serverTimestamp(),
  });
  for (const variant of item.variants) {
    batch.update(ref.collection('variants').doc(variant.id), {
      image,
      updatedAt: FieldValue.serverTimestamp(),
    });
  }
}
for (const { item, ref } of banners) {
  const image = await upload(item.imageAsset, `catalog/banners/${item.id}-v2.jpg`, item.title);
  batch.update(ref, { image, updatedAt: FieldValue.serverTimestamp() });
}
await batch.commit();
console.log(`Published ${products.length} product images and ${banners.length} banners to staging. Inventory and pricing were not changed.`);

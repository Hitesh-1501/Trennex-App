import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { inspectCatalogImage, validateCatalogAsset } from '../scripts/catalog-image-validation.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const assets = path.join(root, 'firebase-data', 'assets');
const drawables = path.join(root, 'app', 'src', 'main', 'res', 'drawable');
const catalog = JSON.parse(await readFile(path.join(root, 'firebase-data', 'catalog.seed.json'), 'utf8'));

test('all 24 current catalog images have matching signatures and safe dimensions', async () => {
  const images = await Promise.all([
    ...catalog.categories.map((item) => validateCatalogAsset(drawables, item.iconAsset)),
    ...catalog.products.map((item) => validateCatalogAsset(assets, item.imageAsset)),
    ...catalog.banners.map((item) => validateCatalogAsset(assets, item.imageAsset)),
  ]);
  assert.equal(images.length, 24);
  assert.ok(images.every((image) => image.width > 0 && image.height > 0));
});

test('rejects misleading extension, truncated content and excessive dimensions', async () => {
  const jpeg = await readFile(path.join(assets, catalog.products[0].imageAsset));
  const png = await readFile(path.join(drawables, catalog.categories[0].iconAsset));
  assert.throws(() => inspectCatalogImage(jpeg, 'fake.png'));
  assert.throws(() => inspectCatalogImage(png, 'fake.jpg'));
  assert.throws(() => inspectCatalogImage(jpeg.subarray(0, 100), 'short.jpg'));
  const tooLarge = Buffer.from(png);
  tooLarge.writeUInt32BE(10000, 16);
  assert.throws(() => inspectCatalogImage(tooLarge, 'oversize.png'));
  assert.throws(() => inspectCatalogImage(Buffer.alloc(10 * 1024 * 1024 + 1), 'large.jpg'));
});

test('asset resolution refuses paths outside the approved source directory', async () => {
  await assert.rejects(validateCatalogAsset(assets, '../../firebase.json'));
});

import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(currentDirectory, '..', '..');
const catalogPath = path.join(repositoryRoot, 'firebase-data', 'catalog.seed.json');
const drawableDirectory = path.join(repositoryRoot, 'app', 'src', 'main', 'res', 'drawable');
const catalog = JSON.parse(await readFile(catalogPath, 'utf8'));

const ids = (items, label) => {
  const values = items.map((item) => item.id);
  assert.equal(new Set(values).size, values.length, `${label} IDs must be unique`);
  return new Set(values);
};

assert.equal(catalog.schemaVersion, 1, 'Catalog schemaVersion must be 1');
assert.equal(catalog.currency, 'INR', 'Catalog currency must be INR');

const categoryIds = ids(catalog.categories, 'Category');
const brandIds = ids(catalog.brands, 'Brand');
const productIds = ids(catalog.products, 'Product');
const legacyIds = catalog.products.map((product) => product.legacyId);
assert.equal(new Set(legacyIds).size, legacyIds.length, 'Product legacyId values must be unique');

let variantCount = 0;
const variantIds = new Set();
for (const product of catalog.products) {
  assert.ok(categoryIds.has(product.categoryId), `${product.id} references a missing category`);
  assert.ok(brandIds.has(product.brandId), `${product.id} references a missing brand`);
  assert.ok(product.variants.length > 0, `${product.id} must have at least one variant`);
  for (const variant of product.variants) {
    assert.ok(!variantIds.has(variant.id), `Variant ID ${variant.id} is duplicated`);
    variantIds.add(variant.id);
    assert.ok(Number.isInteger(variant.priceMinor) && variant.priceMinor > 0, `${variant.id} has invalid priceMinor`);
    assert.ok(Number.isInteger(variant.mrpMinor) && variant.mrpMinor >= variant.priceMinor, `${variant.id} has invalid mrpMinor`);
    assert.ok(Number.isInteger(variant.stock) && variant.stock >= 0, `${variant.id} has invalid stock`);
    variantCount += 1;
  }
}

for (const banner of catalog.banners) {
  assert.ok(banner.id && banner.imageAsset, 'Every banner needs an ID and imageAsset');
}
for (const section of catalog.homeSections) {
  for (const productId of section.productIds || []) {
    assert.ok(productIds.has(productId), `${section.id} references missing product ${productId}`);
  }
}

const assetNames = [
  ...catalog.categories.map((item) => item.iconAsset),
  ...catalog.products.map((item) => item.imageAsset),
  ...catalog.banners.map((item) => item.imageAsset),
];
for (const assetName of new Set(assetNames)) {
  await access(path.join(drawableDirectory, assetName));
}

const summary = {
  categories: catalog.categories.length,
  brands: catalog.brands.length,
  products: catalog.products.length,
  variants: variantCount,
  banners: catalog.banners.length,
  homeSections: catalog.homeSections.length,
  assets: new Set(assetNames).size,
};
console.log('Catalog migration manifest is valid.');
console.table(summary);

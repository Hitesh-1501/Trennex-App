import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getFirestore, Timestamp } from 'firebase-admin/firestore';
import { getStorage } from 'firebase-admin/storage';

const STAGING_PROJECT = 'trennex-staging-ebbeb';
const projectId = process.env.GCLOUD_PROJECT;
if (projectId !== STAGING_PROJECT) {
  throw new Error(`Staging migration refused: GCLOUD_PROJECT must be ${STAGING_PROJECT}.`);
}
if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  throw new Error('Staging migration refused: GOOGLE_APPLICATION_CREDENTIALS is not set.');
}

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(currentDirectory, '..', '..');
const catalog = JSON.parse(await readFile(path.join(repositoryRoot, 'firebase-data', 'catalog.seed.json'), 'utf8'));
const drawableDirectory = path.join(repositoryRoot, 'app', 'src', 'main', 'res', 'drawable');
const bucketName = process.env.STAGING_STORAGE_BUCKET || `${projectId}.firebasestorage.app`;

initializeApp({ credential: applicationDefault(), projectId, storageBucket: bucketName });
const database = getFirestore();
const bucket = getStorage().bucket();
const [bucketExists] = await bucket.exists();
if (!bucketExists) {
  throw new Error(
    `Staging Storage bucket ${bucketName} does not exist. Enable Firebase Storage for ${STAGING_PROJECT} in the Firebase Console, then rerun this command.`,
  );
}
const now = Timestamp.now();
const publishedAt = Timestamp.fromDate(new Date('2026-11-08T00:00:00.000Z'));

const storageUrl = (storagePath) =>
  `https://firebasestorage.googleapis.com/v0/b/${bucketName}/o/${encodeURIComponent(storagePath)}?alt=media`;

async function uploadAsset(assetName, storagePath, altText) {
  await bucket.upload(path.join(drawableDirectory, assetName), {
    destination: storagePath,
    metadata: {
      contentType: assetName.endsWith('.jpg') ? 'image/jpeg' : 'image/png',
      cacheControl: 'public,max-age=3600',
      metadata: { seededBy: 'trennex-staging-catalog-v1' },
    },
  });
  return { storagePath, url: storageUrl(storagePath), altText };
}

const batch = database.batch();
let variantCount = 0;
let homeItemCount = 0;

for (const category of catalog.categories) {
  const icon = await uploadAsset(category.iconAsset, `catalog/categories/${category.id}${path.extname(category.iconAsset)}`, `${category.name} category`);
  batch.set(database.doc(`categories/${category.id}`), {
    schemaVersion: catalog.schemaVersion, slug: category.slug, name: category.name,
    parentId: null, path: [category.id], level: 0, icon,
    description: `Browse ${category.name.toLowerCase()} products on TrenNex.`, active: true,
    sortOrder: category.sortOrder, createdAt: now, updatedAt: now,
  });
}

for (const brand of catalog.brands) {
  batch.set(database.doc(`brands/${brand.id}`), {
    schemaVersion: catalog.schemaVersion, slug: brand.slug, name: brand.name,
    description: `${brand.name} products available on TrenNex.`, active: true,
    sortOrder: brand.sortOrder, createdAt: now, updatedAt: now,
  });
}

for (const product of catalog.products) {
  const image = await uploadAsset(product.imageAsset, `catalog/products/${product.id}/primary${path.extname(product.imageAsset)}`, product.name);
  const prices = product.variants.map((variant) => variant.priceMinor);
  const mrps = product.variants.map((variant) => variant.mrpMinor);
  const minPriceMinor = Math.min(...prices);
  const minMrpMinor = Math.min(...mrps);
  const totalStock = product.variants.reduce((total, variant) => total + variant.stock, 0);
  batch.set(database.doc(`products/${product.id}`), {
    schemaVersion: catalog.schemaVersion, legacyId: product.legacyId, slug: product.slug,
    name: product.name, normalizedName: product.name.toLowerCase(), shortDescription: product.shortDescription,
    description: product.description, brandId: product.brandId, categoryId: product.categoryId,
    categoryPath: [product.categoryId], tags: product.tags,
    searchTerms: [...new Set([...product.name.toLowerCase().split(/\s+/), ...product.tags])],
    thumbnail: image, images: [{ ...image, sortOrder: 1 }], currency: catalog.currency,
    minPriceMinor, maxPriceMinor: Math.max(...prices), minMrpMinor,
    discountPercent: Math.round((1 - minPriceMinor / minMrpMinor) * 1000) / 10,
    ratingAverage: product.ratingAverage, ratingCount: product.ratingCount, reviewCount: product.ratingCount,
    availability: totalStock > 10 ? 'IN_STOCK' : totalStock > 0 ? 'LOW_STOCK' : 'OUT_OF_STOCK',
    shippingInformation: product.shippingInformation, warrantyInformation: product.warrantyInformation,
    returnPolicy: product.returnPolicy, specifications: product.specifications, status: 'ACTIVE',
    featured: product.featured, newArrival: product.newArrival, sortOrder: product.sortOrder,
    createdAt: now, updatedAt: now, publishedAt,
  });
  for (const [index, variant] of product.variants.entries()) {
    variantCount += 1;
    const imagePath = image.storagePath;
    batch.set(database.doc(`products/${product.id}/variants/${variant.id}`), {
      schemaVersion: catalog.schemaVersion, productId: product.id, sku: variant.sku, name: variant.name,
      attributes: variant.attributes, priceMinor: variant.priceMinor, mrpMinor: variant.mrpMinor,
      currency: catalog.currency, image: { ...image, storagePath: imagePath }, active: true,
      sortOrder: index + 1, createdAt: now, updatedAt: now,
    });
    batch.set(database.doc(`inventory/${variant.id}`), {
      schemaVersion: catalog.schemaVersion, productId: product.id, variantId: variant.id,
      onHand: variant.stock, reserved: 0, available: variant.stock, lowStockThreshold: 5, updatedAt: now,
    });
  }
}

for (const banner of catalog.banners) {
  const image = await uploadAsset(banner.imageAsset, `catalog/banners/${banner.id}${path.extname(banner.imageAsset)}`, banner.title);
  batch.set(database.doc(`banners/${banner.id}`), {
    schemaVersion: catalog.schemaVersion, title: banner.title, subtitle: banner.subtitle, image,
    actionType: banner.actionType, actionValue: banner.actionValue, placement: banner.placement,
    active: true, sortOrder: banner.sortOrder, createdAt: now, updatedAt: now,
  });
}

for (const section of catalog.homeSections) {
  batch.set(database.doc(`homeSections/${section.id}`), {
    schemaVersion: catalog.schemaVersion, title: section.title, type: section.type, active: true,
    sortOrder: section.sortOrder, maxItems: section.maxItems, queryConfig: {}, createdAt: now, updatedAt: now,
  });
  for (const [index, productId] of (section.productIds || []).entries()) {
    homeItemCount += 1;
    batch.set(database.doc(`homeSections/${section.id}/items/${productId}`), {
      productId, sortOrder: index + 1, active: true, createdAt: now,
    });
  }
}

batch.set(database.doc('appConfig/public'), {
  schemaVersion: catalog.schemaVersion, minimumSupportedVersionCode: 1, maintenanceMode: false,
  supportEmail: 'support@trennex.example',
  featureFlags: { firebaseCatalog: true, firebaseCheckout: false, reviews: false },
  legalVersions: { privacy: 1, terms: 1 }, updatedAt: now,
});

await batch.commit();
console.log(`Staging catalog migrated to ${projectId}.`);
console.table({ categories: catalog.categories.length, brands: catalog.brands.length, products: catalog.products.length, variants: variantCount, banners: catalog.banners.length, homeSections: catalog.homeSections.length, homeItems: homeItemCount });

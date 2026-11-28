import { after, before, beforeEach, describe, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteObject, getBytes, ref, uploadBytes } from 'firebase/storage';
import { doc, setDoc } from 'firebase/firestore';

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(currentDirectory, '..', '..');
const projectId = 'demo-trennex';
const [firestoreHost, firestorePort] =
  (process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080').split(':');
const [storageHost, storagePort] =
  (process.env.FIREBASE_STORAGE_EMULATOR_HOST || '127.0.0.1:9199').split(':');
let testEnvironment;

const onePixelPng = Uint8Array.from([
  137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82,
  0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 31, 21, 196, 137,
]);

const storageFor = (uid, claims = {}) =>
  testEnvironment.authenticatedContext(uid, claims).storage();
const publicStorage = () => testEnvironment.unauthenticatedContext().storage();
const adminStorage = () => storageFor('admin-user', { trennexRole: 'admin' });

async function seedGrant(active, scopes) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'adminRoleGrants/admin-user'), {
      targetUid: 'admin-user', version: 1, operationState: 'SUCCEEDED', active, scopes,
    });
  });
}

async function seedRawGrant(grant) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'adminRoleGrants/admin-user'), grant);
  });
}

before(async () => {
  const rules = await readFile(path.join(repositoryRoot, 'storage.rules'), 'utf8');
  testEnvironment = await initializeTestEnvironment({
    projectId,
    firestore: { rules: await readFile(path.join(repositoryRoot, 'firestore.rules'), 'utf8'),
      host: firestoreHost, port: Number(firestorePort) },
    storage: { rules, host: storageHost, port: Number(storagePort) },
  });
});

beforeEach(async () => {
  await testEnvironment.clearFirestore();
  await seedGrant(true, ['media']);
});

after(async () => {
  await testEnvironment.cleanup();
});

describe('catalog assets', () => {
  test('only admins upload catalog images and catalog reads are public', async () => {
    const objectPath = 'catalog/products/shirt-1/primary.png';
    await assertFails(uploadBytes(ref(storageFor('buyer-1'), objectPath), onePixelPng, {
      contentType: 'image/png',
    }));
    await assertSucceeds(uploadBytes(ref(adminStorage(), objectPath), onePixelPng, {
      contentType: 'image/png',
    }));
    await assertSucceeds(getBytes(ref(publicStorage(), objectPath)));
  });
});

describe('B4 storage boundaries', () => {
  test('unknown paths, unsafe content types, and non-admin catalog writes fail', async () => {
    const buyer = storageFor('buyer-1');
    const administrator = adminStorage();
    await assertFails(uploadBytes(ref(buyer, 'catalog/banners/sale.png'), onePixelPng,
      { contentType: 'image/png' }));
    await assertFails(uploadBytes(ref(administrator, 'catalog/banners/sale.svg'), onePixelPng,
      { contentType: 'image/svg+xml' }));
    await assertFails(uploadBytes(ref(administrator, 'catalog/private/secret.png'), onePixelPng,
      { contentType: 'image/png' }));
    await assertFails(uploadBytes(ref(buyer, 'unknown/file.png'), onePixelPng,
      { contentType: 'image/png' }));
    await assertFails(uploadBytes(ref(administrator, 'orders/buyer-1/proof.png'), onePixelPng,
      { contentType: 'image/png' }));
    await assertFails(getBytes(ref(publicStorage(), 'users/buyer-1/avatar.png')));
  });

  test('another customer cannot overwrite or delete private files', async () => {
    const ownPath = 'users/buyer-1/avatar.png';
    await assertSucceeds(uploadBytes(ref(storageFor('buyer-1'), ownPath), onePixelPng,
      { contentType: 'image/png' }));
    await assertFails(uploadBytes(ref(storageFor('buyer-2'), ownPath), onePixelPng,
      { contentType: 'image/png' }));
    await assertFails(deleteObject(ref(storageFor('buyer-2'), ownPath)));
    await assertFails(uploadBytes(ref(storageFor('buyer-1'), 'users/buyer-1/other.png'), onePixelPng,
      { contentType: 'image/png' }));
  });
});

describe('B5 media role', () => {
  test('legacy claim and admin role without media scope cannot upload', async () => {
    const objectPath = 'catalog/banners/role-test.png';
    await assertFails(uploadBytes(ref(storageFor('legacy-admin', { admin: true }), objectPath),
      onePixelPng, { contentType: 'image/png' }));
    await seedGrant(true, ['catalog']);
    await assertFails(uploadBytes(ref(adminStorage(), objectPath), onePixelPng,
      { contentType: 'image/png' }));
  });

  test('revoked grant blocks stale admin token and admin cannot read private avatar', async () => {
    const objectPath = 'catalog/banners/revocation-test.png';
    await assertSucceeds(uploadBytes(ref(adminStorage(), objectPath), onePixelPng,
      { contentType: 'image/png' }));
    await assertSucceeds(uploadBytes(ref(storageFor('buyer-1'), 'users/buyer-1/avatar.png'),
      onePixelPng, { contentType: 'image/png' }));
    await assertFails(getBytes(ref(adminStorage(), 'users/buyer-1/avatar.png')));
    await seedGrant(false, ['media']);
    await assertFails(uploadBytes(ref(adminStorage(), 'catalog/banners/after-revoke.png'),
      onePixelPng, { contentType: 'image/png' }));
  });

  test('pending and mismatched grants deny media uploads', async () => {
    const objectPath = 'catalog/banners/denied-grant.png';
    for (const grant of [
      { targetUid: 'admin-user', version: 2, operationState: 'PENDING',
        active: true, scopes: ['media'] },
      { targetUid: 'other-user', version: 2, operationState: 'SUCCEEDED',
        active: true, scopes: ['media'] },
      { targetUid: 'admin-user', version: 0, operationState: 'SUCCEEDED',
        active: true, scopes: ['media'] },
    ]) {
      await seedRawGrant(grant);
      await assertFails(uploadBytes(ref(adminStorage(), objectPath), onePixelPng,
        { contentType: 'image/png' }));
    }
  });
});

describe('private user assets', () => {
  test('users can manage only their own image files', async () => {
    const ownPath = 'users/buyer-1/avatar.png';
    await assertSucceeds(uploadBytes(ref(storageFor('buyer-1'), ownPath), onePixelPng, {
      contentType: 'image/png',
    }));
    await assertFails(getBytes(ref(storageFor('buyer-2'), ownPath)));
    await assertSucceeds(deleteObject(ref(storageFor('buyer-1'), ownPath)));
  });

  test('non-image uploads are rejected', async () => {
    await assertFails(uploadBytes(ref(storageFor('buyer-1'), 'users/buyer-1/profile.txt'), Uint8Array.from([1, 2, 3]), {
      contentType: 'text/plain',
    }));
  });
});

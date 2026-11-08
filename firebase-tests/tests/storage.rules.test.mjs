import { after, before, describe, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteObject, getBytes, ref, uploadBytes } from 'firebase/storage';

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(currentDirectory, '..', '..');
const projectId = 'demo-trennex';
let testEnvironment;

const onePixelPng = Uint8Array.from([
  137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82,
  0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 31, 21, 196, 137,
]);

const storageFor = (uid, claims = {}) =>
  testEnvironment.authenticatedContext(uid, claims).storage();
const publicStorage = () => testEnvironment.unauthenticatedContext().storage();

before(async () => {
  const rules = await readFile(path.join(repositoryRoot, 'storage.rules'), 'utf8');
  testEnvironment = await initializeTestEnvironment({
    projectId,
    storage: { rules, host: '127.0.0.1', port: 9199 },
  });
});

after(async () => {
  await testEnvironment.cleanup();
});

describe('catalog assets', () => {
  test('only admins upload catalog images and catalog reads are public', async () => {
    const objectPath = 'catalog/products/shirt-1.png';
    await assertFails(uploadBytes(ref(storageFor('buyer-1'), objectPath), onePixelPng, {
      contentType: 'image/png',
    }));
    await assertSucceeds(uploadBytes(ref(storageFor('admin-user', { admin: true }), objectPath), onePixelPng, {
      contentType: 'image/png',
    }));
    await assertSucceeds(getBytes(ref(publicStorage(), objectPath)));
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

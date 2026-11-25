import { readFile } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { initializeApp, applicationDefault, deleteApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';

const projectId = 'trennex-staging-ebbeb';
if (process.env.GCLOUD_PROJECT !== projectId || !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  throw new Error('Set staging GCLOUD_PROJECT and GOOGLE_APPLICATION_CREDENTIALS first.');
}
const config = JSON.parse(await readFile(new URL('../../app/src/staging/google-services.json', import.meta.url)));
const apiKey = config.client[0].api_key[0].current_key;
const app = initializeApp({ credential: applicationDefault(), projectId });
const uid = `issue6-smoke-${randomUUID()}`;

try {
  const customToken = await getAuth(app).createCustomToken(uid);
  const signIn = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token: customToken, returnSecureToken: true }),
  });
  const identity = await signIn.json();
  if (!signIn.ok) throw new Error(`Staging sign-in failed: ${signIn.status} ${identity.error?.message}`);

  const response = await fetch(`https://asia-south1-${projectId}.cloudfunctions.net/askShoppingAssistant`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${identity.idToken}` },
    body: JSON.stringify({ data: { question: 'Tell me about the Samsung phone' } }),
  });
  const data = await response.json();
  console.log(JSON.stringify({ status: response.status,
    answer: data.result?.answer, products: data.result?.products?.map((product) => product.id),
    error: data.error?.message }));
  if (!response.ok) process.exitCode = 1;
} finally {
  const day = new Date().toISOString().slice(0, 10);
  await getFirestore(app).doc(`assistantUsage/${day}/users/${uid}`).delete();
  await getAuth(app).deleteUser(uid).catch((error) => {
    if (error.code !== 'auth/user-not-found') throw error;
  });
  await deleteApp(app);
}

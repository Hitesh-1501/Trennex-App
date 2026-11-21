import { readFile } from 'node:fs/promises';

const target = process.argv[2];
const configs = {
  staging: new URL('../../app/src/staging/google-services.json', import.meta.url),
  production: new URL('../../app/google-services.json', import.meta.url),
};
if (!Object.hasOwn(configs, target)) {
  throw new Error('Use: node scripts/audit-public-catalog.mjs staging|production');
}

const config = JSON.parse(await readFile(configs[target], 'utf8'));
const projectId = config.project_info.project_id;
const expected = target === 'production' ? 'trennex-ebbeb' : 'trennex-staging-ebbeb';
if (projectId !== expected) throw new Error(`Unexpected ${target} project ID.`);
const apiKey = config.client?.[0]?.api_key?.[0]?.current_key;
if (!apiKey) throw new Error('The local Firebase client configuration has no API key.');

const base = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
async function getPublic(path) {
  const url = new URL(`${base}/${path}`);
  url.searchParams.set('key', apiKey);
  const response = await fetch(url);
  if (!response.ok) return { read: `HTTP ${response.status}` };
  const document = await response.json();
  return {
    read: 'public',
    schemaVersion: document.fields?.schemaVersion?.integerValue,
    status: document.fields?.status?.stringValue,
    active: document.fields?.active?.booleanValue,
  };
}

// Known IDs are checked individually because Firestore rules can allow a public
// document read while denying an unfiltered collection query.
const seed = JSON.parse(await readFile(new URL('../../firebase-data/catalog.seed.json', import.meta.url), 'utf8'));
const result = { target, projectId, seedSamples: {} };
for (const collection of ['categories', 'brands', 'products', 'banners', 'homeSections']) {
  const id = seed[collection][0].id;
  result.seedSamples[`${collection}/${id}`] = await getPublic(`${collection}/${id}`);
}
const publicConfig = await getPublic('appConfig/public');
result.publicConfig = { read: publicConfig.read };
console.log(JSON.stringify(result, null, 2));

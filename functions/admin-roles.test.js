const { test } = require('node:test');
const assert = require('node:assert/strict');
const { parseRoleCommand, applyRoleChange } = require('./admin-roles');

const stagingEnv = { GCLOUD_PROJECT: 'trennex-staging-ebbeb' };
const base = ['--project=trennex-staging-ebbeb', '--confirm=trennex-staging-ebbeb',
  '--uid=staging-user'];

test('only explicit staging or emulator targets and recognized scopes are accepted', () => {
  assert.deepEqual(parseRoleCommand([...base, '--action=grant', '--scopes=catalog,media'], stagingEnv).scopes,
    ['catalog', 'media']);
  assert.throws(() => parseRoleCommand(['--project=trennex-ebbeb',
    '--confirm=trennex-ebbeb', '--uid=some-user', '--action=grant', '--scopes=catalog'], stagingEnv));
  assert.throws(() => parseRoleCommand([...base, '--action=grant', '--scopes=superuser'], stagingEnv));
  assert.throws(() => parseRoleCommand([...base, '--action=grant', '--action=revoke',
    '--scopes=catalog'], stagingEnv));
  assert.throws(() => parseRoleCommand([...base, '--action=grant'], stagingEnv));
  assert.throws(() => parseRoleCommand([...base, '--action=revoke', '--scopes=catalog'], stagingEnv));
  assert.throws(() => parseRoleCommand([...base.slice(0, 1), '--uid=staging-user',
    '--action=grant', '--scopes=catalog'], stagingEnv));
  assert.throws(() => parseRoleCommand([...base, '--action=grant', '--scopes=catalog'],
    { GCLOUD_PROJECT: 'trennex-ebbeb' }));
  assert.throws(() => parseRoleCommand([...base, '--action=grant', '--scopes=catalog'],
    { FIRESTORE_EMULATOR_HOST: 'localhost:8080' }));
  assert.throws(() => parseRoleCommand(['--project=demo-trennex', '--confirm=demo-trennex',
    '--uid=local-user', '--action=grant', '--scopes=catalog'], {}));
});

function fakeServices({ disabled = false, failAt = null } = {}) {
  const calls = [];
  const auth = {
    getUser: async () => ({ disabled, customClaims: { otherClaim: 'retained', admin: true } }),
    setCustomUserClaims: async (_uid, claims) => {
      calls.push(['claims', claims]);
      if (failAt === 'claims') throw new Error('auth failed');
    },
    revokeRefreshTokens: async () => { calls.push(['tokens']); },
  };
  const db = {
    doc: (path) => path,
    collection: (path) => ({ doc: () => `${path}/generated` }),
    batch: () => ({
      set: (path, value) => calls.push(['set', path, value]),
      commit: async () => {
        calls.push(['commit']);
        if (failAt === 'commit') throw new Error('firestore failed');
      },
    }),
  };
  return { auth, db, calls };
}

test('grant writes server claims before activating scoped grant and audit', async () => {
  const services = fakeServices();
  const result = await applyRoleChange({ ...services,
    command: { uid: 'u1', action: 'grant', scopes: ['catalog'] },
    serverTimestamp: () => 'server-time' });
  assert.equal(result, 'granted');
  assert.deepEqual(services.calls.map(([name]) => name), ['claims', 'set', 'set', 'commit']);
  assert.deepEqual(services.calls[0][1], { otherClaim: 'retained', trennexRole: 'admin' });
  assert.deepEqual(services.calls[1][2], { active: true, scopes: ['catalog'], updatedAt: 'server-time' });
  assert.equal(services.calls[2][1], 'adminAuditLogs/generated');
});

test('revocation disables grant before clearing claim and refresh tokens', async () => {
  const services = fakeServices();
  const result = await applyRoleChange({ ...services,
    command: { uid: 'u1', action: 'revoke', scopes: [] },
    serverTimestamp: () => 'server-time' });
  assert.equal(result, 'revoked');
  assert.deepEqual(services.calls.map(([name]) => name), ['set', 'set', 'commit', 'claims', 'tokens']);
  assert.equal(services.calls[0][2].active, false);
  assert.deepEqual(services.calls[3][1], { otherClaim: 'retained' });
});

test('failed revocation remains denied by grant and does not refresh tokens', async () => {
  const services = fakeServices({ failAt: 'claims' });
  await assert.rejects(applyRoleChange({ ...services,
    command: { uid: 'u1', action: 'revoke', scopes: [] }, serverTimestamp: () => 'server-time' }));
  assert.deepEqual(services.calls.map(([name]) => name), ['set', 'set', 'commit', 'claims']);
});

test('failed grant does not activate the grant', async () => {
  const services = fakeServices({ failAt: 'claims' });
  await assert.rejects(applyRoleChange({ ...services,
    command: { uid: 'u1', action: 'grant', scopes: ['catalog'] },
    serverTimestamp: () => 'server-time' }));
  assert.deepEqual(services.calls.map(([name]) => name), ['claims']);
});

test('disabled accounts cannot receive admin grants', async () => {
  const services = fakeServices({ disabled: true });
  await assert.rejects(applyRoleChange({ ...services,
    command: { uid: 'u1', action: 'grant', scopes: ['catalog'] },
    serverTimestamp: () => 'server-time' }));
  assert.equal(services.calls.length, 0);
});

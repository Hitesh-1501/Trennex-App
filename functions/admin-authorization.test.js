const { test } = require('node:test');
const assert = require('node:assert/strict');
const { requireAdminScope } = require('./admin-authorization');

function fixture(overrides = {}) {
  const grant = { targetUid: 'admin-1', active: true, operationState: 'SUCCEEDED',
    version: 1, scopes: ['catalog'] };
  const decoded = { uid: 'admin-1', trennexRole: 'admin' };
  const request = { auth: { uid: 'admin-1', token: { trennexRole: 'admin' } },
    rawRequest: { headers: { authorization: 'Bearer trusted.token' } } };
  let checkRevoked;
  const services = {
    auth: { verifyIdToken: async (_token, revoked) => {
      checkRevoked = revoked;
      if (overrides.verifyError) throw overrides.verifyError;
      return { ...decoded, ...overrides.decoded };
    } },
    db: { doc: () => ({ get: async () => ({ exists: overrides.exists ?? true,
      data: () => ({ ...grant, ...overrides.grant }) }) }) },
  };
  return { request: { ...request, ...overrides.request }, services,
    checkedRevoked: () => checkRevoked };
}

test('requires verified non-revoked admin token plus exact active grant scope', async () => {
  const f = fixture();
  assert.deepEqual(await requireAdminScope(f.request, 'catalog', f.services),
    { uid: 'admin-1', scope: 'catalog', grantVersion: 1 });
  assert.equal(f.checkedRevoked(), true);
});

test('rejects absent, stale, disabled, forged and wrong-scope identities', async () => {
  const cases = [
    { request: { auth: null } },
    { request: { auth: { uid: 'admin-1', token: {} } } },
    { decoded: { uid: 'another-user' } },
    { decoded: { trennexRole: undefined } },
    { verifyError: new Error('revoked or disabled') },
    { exists: false },
    { grant: { targetUid: 'another-user' } },
    { grant: { active: false } },
    { grant: { operationState: 'PENDING' } },
    { grant: { version: 0 } },
    { grant: { scopes: ['orders'] } },
    { grant: { scopes: ['catalog', 'catalog'] } },
    { grant: { scopes: ['catalog', 'unknown'] } },
  ];
  for (const item of cases) {
    const f = fixture(item);
    await assert.rejects(requireAdminScope(f.request, 'catalog', f.services),
      { code: 'permission-denied' });
  }
  const f = fixture();
  await assert.rejects(requireAdminScope(f.request, 'unknown', f.services),
    { code: 'permission-denied' });
});

const { test } = require('node:test');
const assert = require('node:assert/strict');
const { parseRoleCommand, applyRoleChange } = require('./admin-roles');

const stagingEnv = { GCLOUD_PROJECT: 'trennex-staging-ebbeb' };
const base = ['--project=trennex-staging-ebbeb', '--confirm=trennex-staging-ebbeb',
  '--uid=staging-user'];
const grant = (operationId, expectedVersion, scopes = ['catalog']) => ({
  uid: 'u1', action: 'grant', operationId, expectedVersion, scopes,
});
const revoke = (operationId) => ({ uid: 'u1', action: 'revoke', operationId, scopes: [] });

class MemoryDb {
  constructor() { this.rows = new Map(); this.tail = Promise.resolve(); this.failNextCommit = false; }
  doc(path) { return path; }
  async runTransaction(fn) {
    let release;
    const next = new Promise((resolve) => { release = resolve; });
    const prior = this.tail;
    this.tail = next;
    await prior;
    const writes = [];
    const transaction = {
      get: async (path) => ({ exists: this.rows.has(path), data: () => structuredClone(this.rows.get(path)) }),
      create: (path, value) => writes.push(['create', path, value]),
      set: (path, value, options) => writes.push(['set', path, value, options]),
      update: (path, value) => writes.push(['update', path, value]),
    };
    try {
      const result = await fn(transaction);
      if (this.failNextCommit) { this.failNextCommit = false; throw new Error('Firestore unavailable'); }
      for (const [kind, path, value, options] of writes) {
        if (kind === 'create' && this.rows.has(path)) throw new Error('Audit ID already exists');
        if (kind === 'update' && !this.rows.has(path)) throw new Error('Missing audit or grant');
        this.rows.set(path, options?.merge || kind === 'update'
          ? { ...(this.rows.get(path) || {}), ...value } : structuredClone(value));
      }
      return result;
    } finally { release(); }
  }
  grant() { return this.rows.get('adminRoleGrants/u1'); }
  audit(operationId) { return this.rows.get(`adminAuditLogs/${operationId}`); }
}

function services({ disabled = false, missing = false } = {}) {
  const db = new MemoryDb();
  const user = { disabled, customClaims: { otherClaim: 'retained', admin: true } };
  const auth = {
    failSet: false,
    afterSet: null,
    refreshRevocations: 0,
    async getUser() {
      if (missing) throw Object.assign(new Error('missing'), { code: 'auth/user-not-found' });
      return structuredClone(user);
    },
    async setCustomUserClaims(_uid, claims) {
      if (this.failSet) throw new Error('Auth unavailable');
      if (this.afterSet) await this.afterSet(claims);
      user.customClaims = structuredClone(claims);
    },
    async revokeRefreshTokens() { this.refreshRevocations += 1; },
  };
  return { auth, db, serverTimestamp: () => 'server-time', user };
}

test('CLI refuses production, duplicate arguments, unknown scopes and missing concurrency fields', () => {
  assert.deepEqual(parseRoleCommand([...base, '--action=grant', '--operation-id=operation123',
    '--expected-version=0', '--scopes=catalog,media'], stagingEnv).scopes, ['catalog', 'media']);
  for (const args of [
    ['--project=trennex-ebbeb', '--confirm=trennex-ebbeb', '--uid=u1', '--action=revoke',
      '--operation-id=operation123'],
    [...base, '--action=grant', '--operation-id=operation123', '--scopes=catalog'],
    [...base, '--action=grant', '--operation-id=operation123', '--expected-version=0', '--scopes=admin'],
    [...base, '--action=revoke', '--operation-id=operation123', '--scopes=catalog'],
    [...base, '--action=grant', '--action=revoke', '--operation-id=operation123',
      '--expected-version=0', '--scopes=catalog'],
  ]) assert.throws(() => parseRoleCommand(args, stagingEnv));
  assert.throws(() => parseRoleCommand([...base, '--action=revoke',
    '--operation-id=operation123'], { GCLOUD_PROJECT: 'trennex-ebbeb' }));
  assert.throws(() => parseRoleCommand([...base, '--action=revoke',
    '--operation-id=operation123'], { FIRESTORE_EMULATOR_HOST: 'localhost:8080' }));
});

test('grant, same-ID retry, scope update and revoke are versioned and audited once', async () => {
  const s = services();
  assert.equal(await applyRoleChange({ ...s, command: grant('grant-first-001', 0) }), 'granted');
  assert.equal(s.db.grant().version, 1);
  assert.deepEqual(s.db.grant().scopes, ['catalog']);
  assert.equal(s.db.audit('grant-first-001').state, 'SUCCEEDED');
  assert.equal(s.db.audit('grant-first-001').actorSource, 'trusted_operator_cli_adc');
  assert.equal(s.user.customClaims.otherClaim, 'retained');
  assert.equal(s.user.customClaims.admin, undefined);
  assert.equal(await applyRoleChange({ ...s, command: grant('grant-first-001', 0) }), 'already-complete');
  assert.equal(s.db.rows.size, 2);
  assert.equal(await applyRoleChange({ ...s,
    command: grant('grant-update-02', 1, ['catalog', 'inventory']) }), 'granted');
  assert.equal(s.db.grant().version, 2);
  assert.deepEqual(s.db.audit('grant-update-02').previousScopes, ['catalog']);
  assert.equal(await applyRoleChange({ ...s, command: revoke('revoke-first01') }), 'revoked');
  assert.equal(s.db.grant().active, false);
  assert.equal(s.user.customClaims.trennexRole, undefined);
  assert.equal(s.auth.refreshRevocations, 1);
  assert.equal(await applyRoleChange({ ...s, command: revoke('revoke-first01') }), 'already-complete');
  assert.equal(s.auth.refreshRevocations, 1);
  assert.equal(s.db.audit('revoke-first01').state, 'SUCCEEDED');
});

test('stale or concurrent grants cannot reactivate a revoked role', async () => {
  const s = services();
  let releaseGrant;
  const pause = new Promise((resolve) => { releaseGrant = resolve; });
  let grantStarted;
  const started = new Promise((resolve) => { grantStarted = resolve; });
  s.auth.afterSet = async (claims) => {
    if (claims.trennexRole === 'admin') { grantStarted(); await pause; }
  };
  const pendingGrant = applyRoleChange({ ...s, command: grant('grant-race-001', 0) });
  await started;
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-race-002', 1) }),
    { code: 'is blocked by an incomplete operation' });
  await applyRoleChange({ ...s, command: revoke('revoke-race01') });
  releaseGrant();
  await assert.rejects(pendingGrant, { code: 'was superseded' });
  assert.equal(s.db.grant().active, false);
  assert.equal(s.db.grant().operationState, 'SUCCEEDED');
  assert.equal(s.db.audit('grant-race-001').state, 'SUPERSEDED');
  assert.equal(s.db.audit('revoke-race01').state, 'SUCCEEDED');
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-race-003', 0) }),
    { code: 'has a stale expected version' });
  assert.equal(s.db.grant().active, false);
});

test('Auth failure leaves an incomplete grant denied, then the same operation recovers', async () => {
  const s = services();
  s.auth.failSet = true;
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-retry-01', 0) }),
    { code: 'is incomplete' });
  assert.equal(s.db.grant().active, false);
  assert.equal(s.db.grant().operationState, 'INCOMPLETE');
  assert.equal(s.db.audit('grant-retry-01').state, 'INCOMPLETE');
  s.auth.failSet = false;
  assert.equal(await applyRoleChange({ ...s, command: grant('grant-retry-01', 0) }), 'granted');
  assert.equal(s.db.grant().active, true);
  assert.equal(s.db.audit('grant-retry-01').state, 'SUCCEEDED');
  assert.equal(s.db.rows.size, 2);
});

test('incomplete revocation stays denied and resumes without duplicate success audit', async () => {
  const s = services();
  await applyRoleChange({ ...s, command: grant('grant-before-r', 0) });
  s.auth.failSet = true;
  await assert.rejects(applyRoleChange({ ...s, command: revoke('revoke-retry1') }),
    { code: 'is incomplete' });
  assert.equal(s.db.grant().active, false);
  s.auth.failSet = false;
  assert.equal(await applyRoleChange({ ...s, command: revoke('revoke-retry1') }), 'revoked');
  assert.equal(s.db.audit('revoke-retry1').state, 'SUCCEEDED');
  assert.equal(s.user.customClaims.trennexRole, undefined);
});

test('deleted account revocation succeeds and disabled account grant fails closed', async () => {
  const deleted = services({ missing: true });
  assert.equal(await applyRoleChange({ ...deleted, command: revoke('revoke-deleted') }),
    'revoked-deleted-account');
  assert.equal(deleted.db.grant().active, false);
  const disabled = services({ disabled: true });
  await assert.rejects(applyRoleChange({ ...disabled, command: grant('grant-disabled', 0) }),
    { code: 'is incomplete' });
  assert.equal(disabled.db.grant().active, false);
});

test('audit write failure never reports a successful grant', async () => {
  const s = services();
  s.db.failNextCommit = true;
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-audit-fail', 0) }));
  assert.equal(s.db.grant(), undefined);
  assert.equal(s.user.customClaims.trennexRole, undefined);
});

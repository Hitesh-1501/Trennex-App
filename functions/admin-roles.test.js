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
const effectiveAdmin = (service) => service.db.grant()?.active === true
  && service.db.grant()?.operationState === 'SUCCEEDED'
  && service.user.customClaims.trennexRole === 'admin';

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
    claimWrites: 0,
    refreshRevocations: 0,
    async getUser() {
      if (missing) throw Object.assign(new Error('missing'), { code: 'auth/user-not-found' });
      return structuredClone(user);
    },
    async setCustomUserClaims(_uid, claims) {
      if (this.failSet) throw new Error('Auth unavailable');
      if (this.afterSet) await this.afterSet(claims);
      this.claimWrites += 1;
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
  await assert.rejects(applyRoleChange({ ...s, command: revoke('revoke-race01') }),
    { code: 'is awaiting an earlier Auth mutation; access is inactive' });
  assert.equal(s.db.grant().active, false);
  assert.equal(s.db.grant().operationState, 'WAITING_FOR_AUTH');
  assert.deepEqual(s.db.audit('revoke-race01').previousScopes, []);
  assert.equal(effectiveAdmin(s), false);
  releaseGrant();
  await assert.rejects(pendingGrant, { code: 'was superseded' });
  assert.equal(s.user.customClaims.trennexRole, 'admin');
  assert.equal(s.db.grant().authMutationOwner, null);
  assert.equal(await applyRoleChange({ ...s, command: revoke('revoke-race01') }), 'revoked');
  assert.equal(s.db.grant().active, false);
  assert.equal(s.db.grant().operationState, 'SUCCEEDED');
  assert.equal(s.user.customClaims.trennexRole, undefined);
  assert.equal(effectiveAdmin(s), false);
  assert.equal(s.db.audit('grant-race-001').state, 'SUPERSEDED');
  assert.equal(s.db.audit('grant-race-001').authState, 'COMPLETED');
  assert.equal(s.db.audit('revoke-race01').state, 'SUCCEEDED');
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-race-003', 0) }),
    { code: 'has a stale expected version' });
  assert.equal(s.db.grant().active, false);
});

test('a delayed revoke blocks a newer grant until Auth reconciliation completes', async () => {
  const s = services();
  await applyRoleChange({ ...s, command: grant('grant-before-revoke', 0) });
  let release;
  const pause = new Promise((resolve) => { release = resolve; });
  let started;
  const entered = new Promise((resolve) => { started = resolve; });
  s.auth.afterSet = async (claims) => {
    if (!claims.trennexRole) { started(); await pause; }
  };
  const pending = applyRoleChange({ ...s, command: revoke('revoke-delayed-01') });
  await entered;
  assert.equal(effectiveAdmin(s), false);
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-too-early', 2) }),
    { code: 'is blocked by an incomplete operation' });
  release();
  assert.equal(await pending, 'revoked');
  assert.equal(s.user.customClaims.trennexRole, undefined);
  assert.equal(await applyRoleChange({ ...s, command: grant('grant-after-revoke', 2) }), 'granted');
  assert.equal(effectiveAdmin(s), true);
  assert.equal(s.user.customClaims.otherClaim, 'retained');
});

test('concurrent retries cannot repeat Auth writes or report duplicate success', async () => {
  const s = services();
  let release;
  const pause = new Promise((resolve) => { release = resolve; });
  let started;
  const entered = new Promise((resolve) => { started = resolve; });
  s.auth.afterSet = async () => { started(); await pause; };
  const first = applyRoleChange({ ...s, command: grant('grant-concurrent', 0) });
  await entered;
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-concurrent', 0) }),
    { code: 'is awaiting an active Auth operation' });
  assert.equal(s.db.audit('grant-concurrent').state, 'PENDING');
  release();
  assert.equal(await first, 'granted');
  assert.equal(await applyRoleChange({ ...s, command: grant('grant-concurrent', 0) }),
    'already-complete');
  assert.equal(s.auth.claimWrites, 1);
  assert.equal(s.db.rows.size, 2);
});

test('operation identity includes original expected version and strict CLI integer', async () => {
  const s = services();
  await applyRoleChange({ ...s, command: grant('grant-identity01', 0) });
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-identity01', 1) }),
    { code: 'conflicts with an existing request' });
  await assert.rejects(applyRoleChange({ ...s, command: {
    ...grant('grant-identity01', 0), uid: 'u2',
  } }));
  assert.equal(s.auth.claimWrites, 1);
  assert.throws(() => parseRoleCommand([...base, '--action=grant',
    '--operation-id=grant-identity02', '--expected-version=0x1', '--scopes=catalog'], stagingEnv));
  assert.throws(() => parseRoleCommand([...base, '--action=grant',
    '--operation-id=grant-identity02', '--expected-version= ', '--scopes=catalog'], stagingEnv));
  await assert.rejects(applyRoleChange({ ...s, command: { ...grant('grant-identity03', 1), uid: 7 } }));
});

test('revoke audit retains last effective scopes when a scope update is superseded', async () => {
  const s = services();
  await applyRoleChange({ ...s, command: grant('grant-initial01', 0) });
  let release;
  const pause = new Promise((resolve) => { release = resolve; });
  let started;
  const entered = new Promise((resolve) => { started = resolve; });
  s.auth.afterSet = async (claims) => {
    if (claims.trennexRole === 'admin') { started(); await pause; }
  };
  const update = applyRoleChange({ ...s,
    command: grant('grant-scope-update', 1, ['catalog', 'inventory']) });
  await entered;
  await assert.rejects(applyRoleChange({ ...s, command: revoke('revoke-scope-update') }),
    { code: 'is awaiting an earlier Auth mutation; access is inactive' });
  assert.deepEqual(s.db.audit('revoke-scope-update').previousScopes, ['catalog']);
  assert.equal(s.db.audit('grant-scope-update').state, 'SUPERSEDED');
  release();
  await assert.rejects(update, { code: 'was superseded' });
  await applyRoleChange({ ...s, command: revoke('revoke-scope-update') });
  assert.equal(s.user.customClaims.trennexRole, undefined);
});

test('stopped-operation recovery requires an exact trusted operator confirmation', async () => {
  const s = services();
  s.db.rows.set('adminRoleGrants/u1', {
    targetUid: 'u1', active: false, scopes: [], version: 2,
    operationId: 'revoke-recover-01', operationState: 'WAITING_FOR_AUTH',
    action: 'REVOKE', requestedScopes: [], requestedExpectedVersion: null,
    previousEffectiveScopes: ['catalog'], authMutationOwner: 'grant-stopped01',
  });
  s.db.rows.set('adminAuditLogs/revoke-recover-01', {
    operationId: 'revoke-recover-01', state: 'WAITING_FOR_AUTH',
  });
  await assert.rejects(applyRoleChange({ ...s, command: revoke('revoke-recover-01') }),
    { code: 'is awaiting an active Auth operation' });
  await assert.rejects(applyRoleChange({ ...s, command: {
    ...revoke('revoke-recover-01'), confirmStoppedOperation: 'wrong-owner01',
  } }), { code: 'has a mismatched stopped-operation confirmation' });
  assert.equal(await applyRoleChange({ ...s, command: {
    ...revoke('revoke-recover-01'), confirmStoppedOperation: 'grant-stopped01',
  } }), 'revoked');
  assert.equal(s.db.grant().authMutationOwner, null);
  assert.equal(s.db.audit('revoke-recover-01').recoveredStoppedOperation, 'grant-stopped01');
  assert.equal(s.db.audit('revoke-recover-01').state, 'SUCCEEDED');
});

test('uncertain Auth failure during supersession retains the lock for manual reconciliation', async () => {
  const s = services();
  let release;
  const pause = new Promise((resolve) => { release = resolve; });
  let started;
  const entered = new Promise((resolve) => { started = resolve; });
  s.auth.afterSet = async (claims) => {
    if (claims.trennexRole === 'admin') {
      started();
      await pause;
      throw new Error('uncertain Auth response');
    }
  };
  const pending = applyRoleChange({ ...s, command: grant('grant-uncertain1', 0) });
  await entered;
  await assert.rejects(applyRoleChange({ ...s, command: revoke('revoke-uncertain1') }),
    { code: 'is awaiting an earlier Auth mutation; access is inactive' });
  release();
  await assert.rejects(pending, { code: 'was superseded with uncertain Auth state' });
  assert.equal(s.db.grant().active, false);
  assert.equal(s.db.grant().authMutationOwner, 'grant-uncertain1');
  assert.equal(s.db.audit('grant-uncertain1').authState, 'UNCERTAIN');
  await assert.rejects(applyRoleChange({ ...s, command: revoke('revoke-uncertain1') }),
    { code: 'is awaiting an active Auth operation' });
  s.auth.afterSet = null;
  assert.equal(await applyRoleChange({ ...s, command: {
    ...revoke('revoke-uncertain1'), confirmStoppedOperation: 'grant-uncertain1',
  } }), 'revoked');
  assert.equal(s.user.customClaims.trennexRole, undefined);
});

test('Auth failure leaves an incomplete grant denied, then the same operation recovers', async () => {
  const s = services();
  s.auth.failSet = true;
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-retry-01', 0) }),
    { code: 'is incomplete' });
  assert.equal(s.db.grant().active, false);
  assert.equal(s.db.grant().operationState, 'INCOMPLETE');
  assert.equal(s.db.grant().authMutationOwner, 'grant-retry-01');
  assert.equal(s.db.audit('grant-retry-01').state, 'INCOMPLETE');
  s.auth.failSet = false;
  await assert.rejects(applyRoleChange({ ...s, command: grant('grant-retry-01', 0) }),
    { code: 'is awaiting an active Auth operation' });
  assert.equal(await applyRoleChange({ ...s, command: {
    ...grant('grant-retry-01', 0), confirmStoppedOperation: 'grant-retry-01',
  } }), 'granted');
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
  await assert.rejects(applyRoleChange({ ...s, command: revoke('revoke-retry1') }),
    { code: 'is awaiting an active Auth operation' });
  assert.equal(await applyRoleChange({ ...s, command: {
    ...revoke('revoke-retry1'), confirmStoppedOperation: 'revoke-retry1',
  } }), 'revoked');
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

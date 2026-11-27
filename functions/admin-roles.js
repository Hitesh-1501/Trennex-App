const STAGING_PROJECT = 'trennex-staging-ebbeb';
const DEMO_PROJECT = 'demo-trennex';
const ADMIN_SCOPES = Object.freeze([
  'catalog', 'inventory', 'orders', 'reviews', 'promotions', 'media', 'audit',
]);

function parseRoleCommand(argv, environment = process.env) {
  const pairs = argv.map((arg) => {
    const match = /^--([a-z-]+)=(.*)$/.exec(arg);
    if (!match) throw new Error('Use --name=value arguments only.');
    return [match[1], match[2]];
  });
  const options = Object.fromEntries(pairs);
  if (Object.keys(options).length !== pairs.length) {
    throw new Error('Duplicate role-management argument.');
  }
  if (Object.keys(options).some((key) => !['project', 'action', 'uid', 'scopes', 'confirm'].includes(key))) {
    throw new Error('Unknown role-management argument.');
  }
  const project = options.project;
  if (project !== STAGING_PROJECT && project !== DEMO_PROJECT) {
    throw new Error('Role changes are restricted to TrenNex staging or the local demo emulator.');
  }
  if (options.confirm !== project) throw new Error('Explicit project confirmation is required.');
  if (project === DEMO_PROJECT
      && (!environment.FIREBASE_AUTH_EMULATOR_HOST || !environment.FIRESTORE_EMULATOR_HOST)) {
    throw new Error('The demo project requires Auth and Firestore emulators.');
  }
  if (project === STAGING_PROJECT
      && (environment.FIREBASE_AUTH_EMULATOR_HOST || environment.FIRESTORE_EMULATOR_HOST)) {
    throw new Error('Do not mix staging credentials with local emulators.');
  }
  if (environment.GCLOUD_PROJECT && environment.GCLOUD_PROJECT !== project) {
    throw new Error('Credential project and requested project differ.');
  }
  if (environment.GOOGLE_CLOUD_PROJECT && environment.GOOGLE_CLOUD_PROJECT !== project) {
    throw new Error('Credential project and requested project differ.');
  }
  if (!/^[A-Za-z0-9_-]{1,128}$/.test(options.uid || '')) {
    throw new Error('A valid Firebase UID is required.');
  }
  if (!['grant', 'revoke'].includes(options.action)) {
    throw new Error('Choose grant or revoke.');
  }
  const scopes = options.scopes ? [...new Set(options.scopes.split(',').filter(Boolean))] : [];
  if (options.action === 'grant' && scopes.length === 0) {
    throw new Error('Grant requires at least one explicit scope.');
  }
  if (options.action === 'revoke' && options.scopes) {
    throw new Error('Revoke removes all scopes; omit --scopes.');
  }
  if (scopes.some((scope) => !ADMIN_SCOPES.includes(scope))) {
    throw new Error('Unknown admin scope.');
  }
  return { project, uid: options.uid, action: options.action, scopes };
}

async function applyRoleChange({ auth, db, command, serverTimestamp }) {
  const { uid, action, scopes } = command;
  const user = await auth.getUser(uid);
  if (action === 'grant' && user.disabled) throw new Error('Cannot grant a disabled account.');
  const claims = { ...(user.customClaims || {}) };
  delete claims.admin; // A legacy boolean is never sufficient for B5 authorization.
  const grantRef = db.doc(`adminRoleGrants/${uid}`);
  const auditRef = db.collection('adminAuditLogs').doc();
  const now = serverTimestamp();

  if (action === 'revoke') {
    // Deny stale ID tokens first. Later Auth failures leave the rules fail-closed.
    const batch = db.batch();
    batch.set(grantRef, { active: false, scopes: [], updatedAt: now });
    batch.set(auditRef, { action: 'REVOKE_ADMIN', subjectUid: uid, createdAt: now });
    await batch.commit();
    delete claims.trennexRole;
    await auth.setCustomUserClaims(uid, claims);
    await auth.revokeRefreshTokens(uid);
    return 'revoked';
  }

  // A claim by itself has no access until the server-owned grant is active.
  claims.trennexRole = 'admin';
  await auth.setCustomUserClaims(uid, claims);
  const batch = db.batch();
  batch.set(grantRef, { active: true, scopes, updatedAt: now });
  batch.set(auditRef, { action: 'GRANT_ADMIN', subjectUid: uid, scopes,
    createdAt: now });
  await batch.commit();
  return 'granted';
}

module.exports = { STAGING_PROJECT, DEMO_PROJECT, ADMIN_SCOPES, parseRoleCommand, applyRoleChange };

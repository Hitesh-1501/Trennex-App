const { ADMIN_SCOPES } = require('./admin-roles');

class AdminAuthorizationError extends Error {
  constructor() {
    super('Administrator access is unavailable.');
    this.code = 'permission-denied';
  }
}

function validGrant(grant, uid, scope) {
  return grant && grant.targetUid === uid && grant.active === true
    && grant.operationState === 'SUCCEEDED'
    && Number.isSafeInteger(grant.version) && grant.version >= 1
    && Array.isArray(grant.scopes) && grant.scopes.length > 0
    && grant.scopes.every((item) => ADMIN_SCOPES.includes(item))
    && new Set(grant.scopes).size === grant.scopes.length
    && grant.scopes.includes(scope);
}

// For future onCall admin endpoints only. The Admin SDK bypasses security rules.
// request.auth must be supplied by Firebase Functions, never by caller data.
async function requireAdminScope(request, scope, { auth, db }) {
  if (!ADMIN_SCOPES.includes(scope)) throw new AdminAuthorizationError();
  const uid = request?.auth?.uid;
  const header = request?.rawRequest?.headers?.authorization;
  if (typeof uid !== 'string' || request.auth.token?.trennexRole !== 'admin'
      || typeof header !== 'string' || !/^Bearer [A-Za-z0-9._-]+$/.test(header)) {
    throw new AdminAuthorizationError();
  }
  try {
    // checkRevoked also rejects disabled accounts; compare verified UID and claim.
    const decoded = await auth.verifyIdToken(header.slice(7), true);
    if (decoded.uid !== uid || decoded.trennexRole !== 'admin') {
      throw new AdminAuthorizationError();
    }
    const snapshot = await db.doc(`adminRoleGrants/${uid}`).get();
    if (!snapshot.exists || !validGrant(snapshot.data(), uid, scope)) {
      throw new AdminAuthorizationError();
    }
    return { uid, scope, grantVersion: snapshot.data().version };
  } catch {
    throw new AdminAuthorizationError();
  }
}

module.exports = { AdminAuthorizationError, validGrant, requireAdminScope };

const STAGING_PROJECT = 'trennex-staging-ebbeb';
const DEMO_PROJECT = 'demo-trennex';
const ADMIN_SCOPES = Object.freeze([
  'catalog', 'inventory', 'orders', 'reviews', 'promotions', 'media', 'audit',
]);
const OPERATION_ID = /^[A-Za-z0-9_-]{10,128}$/;
const UID = /^[A-Za-z0-9_-]{1,128}$/;

class RoleOperationError extends Error {
  constructor(code, operationId) {
    super(`Admin role operation ${operationId} ${code}. Inspect its server-owned audit state before retrying.`);
    this.code = code;
    this.operationId = operationId;
  }
}

function validateCommand(command) {
  const { uid, action, operationId, scopes, expectedVersion, confirmStoppedOperation } = command;
  if (typeof uid !== 'string' || !UID.test(uid)
      || typeof operationId !== 'string' || !OPERATION_ID.test(operationId)) {
    throw new Error('Valid UID and operation ID are required.');
  }
  if (confirmStoppedOperation !== undefined
      && (typeof confirmStoppedOperation !== 'string'
        || !OPERATION_ID.test(confirmStoppedOperation))) {
    throw new Error('Stopped-operation confirmation must name an operation ID.');
  }
  if (action !== 'grant' && action !== 'revoke') throw new Error('Choose grant or revoke.');
  if (!Array.isArray(scopes) || scopes.some((scope) => !ADMIN_SCOPES.includes(scope))
      || new Set(scopes).size !== scopes.length) throw new Error('Invalid admin scopes.');
  if (action === 'grant' && (scopes.length === 0 || !Number.isSafeInteger(expectedVersion)
      || expectedVersion < 0)) throw new Error('Grant requires scopes and an expected version.');
  if (action === 'revoke' && (scopes.length || expectedVersion !== undefined)) {
    throw new Error('Revoke removes all scopes; omit scopes and expected version.');
  }
  return { uid, action, operationId, scopes, expectedVersion, confirmStoppedOperation };
}

function parseRoleCommand(argv, environment = process.env) {
  const pairs = argv.map((arg) => {
    const match = /^--([a-z-]+)=(.*)$/.exec(arg);
    if (!match) throw new Error('Use --name=value arguments only.');
    return [match[1], match[2]];
  });
  const options = Object.fromEntries(pairs);
  if (Object.keys(options).length !== pairs.length
      || Object.keys(options).some((key) => ![
        'project', 'action', 'uid', 'scopes', 'confirm', 'operation-id', 'expected-version',
        'confirm-stopped-operation',
      ].includes(key))) throw new Error('Unknown or duplicate role-management argument.');
  const project = options.project;
  if (project !== STAGING_PROJECT && project !== DEMO_PROJECT) {
    throw new Error('Role changes are restricted to staging or the local demo emulator.');
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
  if ((environment.GCLOUD_PROJECT && environment.GCLOUD_PROJECT !== project)
      || (environment.GOOGLE_CLOUD_PROJECT && environment.GOOGLE_CLOUD_PROJECT !== project)) {
    throw new Error('Credential project and requested project differ.');
  }
  const scopes = options.scopes ? options.scopes.split(',') : [];
  const rawVersion = options['expected-version'];
  if (rawVersion !== undefined && !/^(0|[1-9][0-9]*)$/.test(rawVersion)) {
    throw new Error('Expected version must be a nonnegative decimal integer.');
  }
  const expectedVersion = rawVersion === undefined ? undefined : Number(rawVersion);
  return { project, ...validateCommand({
    uid: options.uid, action: options.action, operationId: options['operation-id'],
    scopes, expectedVersion, confirmStoppedOperation: options['confirm-stopped-operation'],
  }) };
}

function sameRequest(grant, command) {
  return grant.operationId === command.operationId
    && grant.action === command.action.toUpperCase()
    && JSON.stringify(grant.requestedScopes || []) === JSON.stringify(command.scopes)
    && (grant.requestedExpectedVersion ?? null) === (command.expectedVersion ?? null);
}

async function reserveOperation(db, command, now) {
  const grantRef = db.doc(`adminRoleGrants/${command.uid}`);
  const auditRef = db.doc(`adminAuditLogs/${command.operationId}`);
  return db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(grantRef);
    const current = snapshot.exists ? snapshot.data() : {};
    const version = Number.isSafeInteger(current.version) && current.version >= 0
      ? current.version : 0;
    if (current.operationId === command.operationId) {
      if (!sameRequest(current, command)) throw new RoleOperationError('conflicts with an existing request', command.operationId);
      if (current.operationState === 'SUCCEEDED') return { state: 'already-complete', version };
      const owner = current.authMutationOwner;
      if (command.confirmStoppedOperation && owner !== command.confirmStoppedOperation) {
        throw new RoleOperationError('has a mismatched stopped-operation confirmation', command.operationId);
      }
      if (owner && owner !== command.confirmStoppedOperation) {
        throw new RoleOperationError('is awaiting an active Auth operation', command.operationId);
      }
      if (!['PENDING', 'INCOMPLETE', 'WAITING_FOR_AUTH'].includes(current.operationState)) {
        throw new RoleOperationError('has an invalid stored state', command.operationId);
      }
      transaction.update(grantRef, {
        authMutationOwner: command.operationId, operationState: 'PENDING', updatedAt: now,
      });
      transaction.update(auditRef, { state: 'PENDING', updatedAt: now,
        ...(owner ? { recoveredStoppedOperation: owner } : {}) });
      return { state: 'resume', version };
    }
    if (command.action === 'grant') {
      if (current.authMutationOwner || ['PENDING', 'INCOMPLETE', 'WAITING_FOR_AUTH'].includes(current.operationState)) {
        throw new RoleOperationError('is blocked by an incomplete operation', command.operationId);
      }
      if (version !== command.expectedVersion) {
        throw new RoleOperationError('has a stale expected version', command.operationId);
      }
    }
    if (command.action === 'revoke' && current.action === 'REVOKE'
        && ['PENDING', 'INCOMPLETE', 'WAITING_FOR_AUTH'].includes(current.operationState)) {
      throw new RoleOperationError('is blocked by an incomplete revocation', command.operationId);
    }
    const nextVersion = version + 1;
    const previousScopes = current.active === true && Array.isArray(current.scopes)
      ? current.scopes : (Array.isArray(current.previousEffectiveScopes) ? current.previousEffectiveScopes : []);
    const waitingFor = current.authMutationOwner || null;
    if (command.confirmStoppedOperation && waitingFor !== command.confirmStoppedOperation) {
      throw new RoleOperationError('has a mismatched stopped-operation confirmation', command.operationId);
    }
    transaction.create(auditRef, {
      operationId: command.operationId, actorSource: 'trusted_operator_cli_adc',
      targetUid: command.uid, action: command.action.toUpperCase(),
      previousScopes, requestedScopes: command.scopes,
      requestedExpectedVersion: command.expectedVersion ?? null,
      state: waitingFor && waitingFor !== command.confirmStoppedOperation ? 'WAITING_FOR_AUTH' : 'PENDING',
      createdAt: now, updatedAt: now,
    });
    if (command.action === 'revoke' && typeof current.operationId === 'string'
        && ['PENDING', 'INCOMPLETE', 'WAITING_FOR_AUTH'].includes(current.operationState)) {
      transaction.set(db.doc(`adminAuditLogs/${current.operationId}`), {
        state: 'SUPERSEDED', supersededBy: command.operationId, updatedAt: now,
      }, { merge: true });
    }
    const mustWait = waitingFor && waitingFor !== command.confirmStoppedOperation;
    transaction.set(grantRef, {
      targetUid: command.uid, active: false, scopes: [], version: nextVersion,
      operationId: command.operationId, operationState: mustWait ? 'WAITING_FOR_AUTH' : 'PENDING',
      action: command.action.toUpperCase(), requestedScopes: command.scopes,
      requestedExpectedVersion: command.expectedVersion ?? null,
      previousEffectiveScopes: previousScopes,
      authMutationOwner: mustWait ? waitingFor : command.operationId,
      updatedAt: now,
    });
    return { state: mustWait ? 'waiting' : 'reserved', version: nextVersion };
  });
}

async function finishOperation(db, command, version, state, now, retainAuthOwner = false) {
  const grantRef = db.doc(`adminRoleGrants/${command.uid}`);
  const auditRef = db.doc(`adminAuditLogs/${command.operationId}`);
  return db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(grantRef);
    const grant = snapshot.exists ? snapshot.data() : {};
    if (grant.operationId !== command.operationId || grant.version !== version) {
      if (grant.authMutationOwner === command.operationId && !retainAuthOwner) {
        transaction.update(grantRef, { authMutationOwner: null, updatedAt: now });
      }
      transaction.update(auditRef, {
        authState: retainAuthOwner ? 'UNCERTAIN' : 'COMPLETED', updatedAt: now,
      });
      return 'superseded';
    }
    if (grant.operationState === 'SUCCEEDED') return 'already-complete';
    if (grant.authMutationOwner !== command.operationId) {
      throw new RoleOperationError('lost its Auth mutation ownership', command.operationId);
    }
    if (state === 'SUCCEEDED') {
      transaction.update(grantRef, { active: command.action === 'grant',
        scopes: command.action === 'grant' ? command.scopes : [],
        operationState: 'SUCCEEDED', authMutationOwner: null, updatedAt: now });
    } else {
      transaction.update(grantRef, { active: false, scopes: [],
        operationState: 'INCOMPLETE',
        authMutationOwner: retainAuthOwner ? command.operationId : null, updatedAt: now });
    }
    transaction.update(auditRef, { state,
      authState: state === 'SUCCEEDED' ? 'COMPLETED'
        : (retainAuthOwner ? 'UNCERTAIN' : 'NOT_MUTATED'),
      updatedAt: now });
    return state;
  });
}

async function applyRoleChange({ auth, db, command: input, serverTimestamp }) {
  const command = validateCommand(input);
  const { uid, operationId } = command;
  const reservation = await reserveOperation(db, command, serverTimestamp());
  if (reservation.state === 'already-complete') {
    return reservation.state;
  }
  if (reservation.state === 'waiting') {
    throw new RoleOperationError('is awaiting an earlier Auth mutation; access is inactive', operationId);
  }
  let result;
  let authMutationStarted = false;
  try {
    let user;
    try {
      user = await auth.getUser(uid);
    } catch (error) {
      if (command.action === 'revoke' && error.code === 'auth/user-not-found') {
        result = 'revoked-deleted-account';
      } else {
        throw error;
      }
    }
    if (user) {
      if (command.action === 'grant' && user.disabled) throw new Error('disabled-user');
      const claims = { ...(user.customClaims || {}) };
      delete claims.admin;
      if (command.action === 'grant') claims.trennexRole = 'admin';
      else delete claims.trennexRole;
      authMutationStarted = true;
      await auth.setCustomUserClaims(uid, claims);
      if (command.action === 'revoke') await auth.revokeRefreshTokens(uid);
      result = command.action === 'grant' ? 'granted' : 'revoked';
    }
  } catch (error) {
    let completion;
    try {
      completion = await finishOperation(db, command, reservation.version, 'INCOMPLETE',
        serverTimestamp(), authMutationStarted);
    } catch {
      // An uncertain completion remains inactive. Retain any Auth owner until reconciled.
    }
    if (completion === 'superseded') {
      throw new RoleOperationError(authMutationStarted
        ? 'was superseded with uncertain Auth state' : 'was superseded', operationId);
    }
    throw new RoleOperationError('is incomplete', operationId);
  }
  let completion;
  try {
    completion = await finishOperation(db, command, reservation.version, 'SUCCEEDED', serverTimestamp());
  } catch {
    throw new RoleOperationError('has uncertain Firestore finalization', operationId);
  }
  if (completion === 'superseded') throw new RoleOperationError('was superseded', operationId);
  return completion === 'already-complete' ? completion : result;
}

module.exports = { STAGING_PROJECT, DEMO_PROJECT, ADMIN_SCOPES, RoleOperationError,
  parseRoleCommand, validateCommand, applyRoleChange };

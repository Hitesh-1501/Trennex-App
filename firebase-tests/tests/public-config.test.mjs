import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildPublicConfig, validatePublicConfig } from '../scripts/public-config.mjs';

const timestamp = { toDate: () => new Date('2026-11-28T00:00:00Z') };

test('the supported publisher builds only approved public fields', () => {
  const config = buildPublicConfig(1, timestamp);
  assert.equal(config.maintenanceMode, false);
  assert.equal(config.supportEmail, 'support@trennex.example');
  assert.deepEqual(Object.keys(config).sort(), [
    'schemaVersion', 'minimumSupportedVersionCode', 'maintenanceMode',
    'supportEmail', 'featureFlags', 'legalVersions', 'updatedAt',
  ].sort());
});

test('public config rejects secret, customer, endpoint and nested extra fields', () => {
  const good = buildPublicConfig(1, timestamp);
  for (const extra of [
    { apiKey: 'secret' }, { customerPhone: 'private' }, { internalEndpoint: 'https://internal' },
  ]) assert.throws(() => validatePublicConfig({ ...good, ...extra }));
  assert.throws(() => validatePublicConfig({ ...good,
    featureFlags: { ...good.featureFlags, adminMode: true } }));
  assert.throws(() => validatePublicConfig({ ...good,
    legalVersions: { ...good.legalVersions, token: 'private' } }));
});

test('public config rejects invalid types, values and unapproved contact', () => {
  const good = buildPublicConfig(1, timestamp);
  assert.throws(() => buildPublicConfig('1', timestamp));
  assert.throws(() => validatePublicConfig({ ...good, maintenanceMode: 'false' }));
  assert.throws(() => validatePublicConfig({ ...good, supportEmail: 'buyer@example.test' }));
  assert.throws(() => validatePublicConfig({ ...good, featureFlags: {
    ...good.featureFlags, firebaseCheckout: 'enabled',
  } }));
  assert.throws(() => validatePublicConfig({ ...good, updatedAt: 'yesterday' }));
});

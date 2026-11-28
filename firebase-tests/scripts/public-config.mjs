const publicFields = [
  'schemaVersion', 'minimumSupportedVersionCode', 'maintenanceMode',
  'supportEmail', 'featureFlags', 'legalVersions', 'updatedAt',
];

function exactFields(value, fields) {
  return value && typeof value === 'object' && !Array.isArray(value)
    && Object.keys(value).length === fields.length
    && fields.every((field) => Object.hasOwn(value, field));
}

export function validatePublicConfig(value) {
  if (!exactFields(value, publicFields)
      || !Number.isSafeInteger(value.schemaVersion) || value.schemaVersion < 1
      || value.schemaVersion > 1000
      || !Number.isSafeInteger(value.minimumSupportedVersionCode)
      || value.minimumSupportedVersionCode < 1
      || value.minimumSupportedVersionCode > 1000000
      || typeof value.maintenanceMode !== 'boolean'
      || value.supportEmail !== 'support@trennex.example'
      || !exactFields(value.featureFlags, ['firebaseCatalog', 'firebaseCheckout', 'reviews'])
      || Object.values(value.featureFlags).some((flag) => typeof flag !== 'boolean')
      || !exactFields(value.legalVersions, ['privacy', 'terms'])
      || Object.values(value.legalVersions).some((version) =>
        !Number.isSafeInteger(version) || version < 1 || version > 1000000)
      || typeof value.updatedAt?.toDate !== 'function') {
    throw new Error('Public appConfig does not match the approved publish schema.');
  }
  return value;
}

export function buildPublicConfig(schemaVersion, updatedAt) {
  return validatePublicConfig({
    schemaVersion,
    minimumSupportedVersionCode: 1,
    maintenanceMode: false,
    supportEmail: 'support@trennex.example',
    featureFlags: { firebaseCatalog: true, firebaseCheckout: false, reviews: false },
    legalVersions: { privacy: 1, terms: 1 },
    updatedAt,
  });
}

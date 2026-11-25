# Catalog migration status

The canonical migration source is:

```text
firebase-data/catalog.seed.json
```

The local catalog contains stable string IDs, preserved numeric `legacyId` values, INR prices in
minor units, variants, inventory, banners, home sections, and drawable assets. The validator checks
all references and asset files without writing to Firebase:

```powershell
cd firebase-tests
npm run validate:catalog
```

## Current environment

`.firebaserc` uses separate project aliases:

```json
{
  "projects": {
    "default": "demo-trennex",
    "staging": "trennex-staging-ebbeb",
    "production": "trennex-ebbeb"
  }
}
```

The default `demo-trennex` project is for local emulators. On 2026-11-14,
`npm run verify:staging` confirmed the staging project contains 10 categories,
8 brands, 11 products, 15 variants, 15 inventory records, 3 banners,
3 home sections, and 24 Storage assets. The production project was not
changed in this verification. A read-only Cloud Functions API request returned
HTTP 403 because the Cloud Functions API is disabled on staging. `createOrder`
therefore needs API enablement and deployment before staging checkout can work.

## Production migration gate

Before any live write, we must:

1. Keep staging and production separate; never replace `demo-trennex`.
2. Run `npm run validate:catalog`, emulator seed verification, Firestore rules tests, and Cloud
   Function integration tests.
3. Review the catalog manifest and asset count.
4. Verify staging rules, indexes, and the `createOrder` deployment before a real-device checkout test.
5. Revalidate the source manifest and staging documents before copying catalog changes.
6. Obtain explicit approval before running the production migration.

The staging Storage bucket is configured and contains the catalog assets.

## Android staging variant

The staging Firebase Android app uses the same package name and is configured through the ignored
`app/src/staging/google-services.json` file. Build it with:

```powershell
./gradlew.bat testStagingUnitTest assembleStaging
```

The `staging` build type connects to `trennex-staging-ebbeb` and uses only the owned catalog when
`USE_FIREBASE_EMULATORS=false`; the existing `debug` and `release` builds continue using the
production `app/google-services.json` configuration and retain their legacy
catalog fallback until production migration is complete.

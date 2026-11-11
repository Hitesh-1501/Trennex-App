# Catalog migration status

Step 9 is currently in the validation phase. The canonical migration source is:

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

`.firebaserc` currently points only to the demo project:

```json
{
  "projects": {
    "default": "demo-trennex"
  }
}
```

The staging project is now configured as `trennex-staging-ebbeb`, and the existing TrenNex project
is explicitly marked as `production` (`trennex-ebbeb`). The default project remains
`demo-trennex`, so emulator scripts continue to use local data. No staging or production catalog
migration has been performed yet.

## Production migration gate

Before any live write, we must:

1. Create a separate staging/production Firebase project and alias; never replace `demo-trennex`.
2. Run `npm run validate:catalog`, emulator seed verification, Firestore rules tests, and Cloud
   Function integration tests.
3. Review the catalog manifest and asset count.
4. Deploy rules and indexes to staging before catalog data.
5. Run a dry-run comparison between the source manifest and staging documents.
6. Obtain explicit approval before running the production migration.

The staging migration script also requires a staging Cloud Storage bucket. The new staging project
does not have one yet, so enable Firebase Storage in the staging Firebase Console before running
`npm run migrate:staging`. The existing emulator seed script remains the only completed local
catalog import path in this phase.

## Android staging variant

The staging Firebase Android app uses the same package name and is configured through the ignored
`app/src/staging/google-services.json` file. Build it with:

```powershell
./gradlew.bat testStagingUnitTest assembleStaging
```

The `staging` build type connects to `trennex-staging-ebbeb` when
`USE_FIREBASE_EMULATORS=false`; the existing `debug` and `release` builds continue using the
production `app/google-services.json` configuration.

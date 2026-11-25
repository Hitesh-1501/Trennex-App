# Production readiness audit

Checked 2026-11-21 on `codex/trennex-development`. This is a read-only assessment. No production Firebase documents, Storage objects, rules, functions, authentication settings, or app releases were changed.

## What TrenNex currently has

- Android app `com.example.trennex` has a staging build connected to `trennex-staging-ebbeb` and a release configuration connected to `trennex-ebbeb`. Both local `google-services.json` files point to their intended projects; their keys are not recorded here.
- Staging uses the owned Firestore catalog only. Its verified baseline is 10 categories, 8 brands, 11 products, 15 variants/inventory records, 3 banners, 3 home sections, and at least 24 catalog Storage objects. `appConfig/public` enables the Firebase catalog. The staging `createOrder` and `askShoppingAssistant` Cloud Functions are active in `asia-south1`.
- The release build still sets `USE_OWN_CATALOG_ONLY=false`, so it can fall back to the older DummyJSON product API. Do not change this flag until the production catalog and dependent flows have passed release testing.
- The source catalog is `firebase-data/catalog.seed.json`; images are under `firebase-data/assets`. Some branded product images are illustrative concepts, not approved exact product photos. Prices, stock, product descriptions, and commercial claims need owner review before production use.

## Read-only production findings

| Check | Result | Meaning |
| --- | --- | --- |
| Firebase project | `trennex-ebbeb` exists and is active | The production project is distinct from staging. |
| Android registration | One active `com.example.trennex` app | The Android package is registered. |
| Local production client configuration | Present and points to `trennex-ebbeb` | The local release build has a production Firebase target. |
| Five known catalog documents and `appConfig/public` | Each REST read returned HTTP 403 | Data presence and deployed rules cannot be determined from this unauthenticated probe. A 403 is **not** an empty catalog. |
| Same five documents in staging | Publicly readable, schema version 1, active | The probe works against the established staging documents. |
| Production Cloud Functions list | Firebase CLI returned `Failed to list functions` | Function presence and deploy permissions remain unverified. This does **not** establish that functions are absent. |
| Production Auth, Storage objects, indexes, deployed rules, order flow | Not inspected | These require authenticated project inspection and release-device checks. |

Run the public document probe from `firebase-tests` with `node scripts/audit-public-catalog.mjs staging` or `production`. It reads only known document IDs and reports HTTP status; it does not list private records or print API keys. Production validation needs a suitable production read credential or a Firebase Console inspection. Never use the staging service-account file for production.

## Release blockers

1. Establish a read-only inventory of existing production collections, catalog IDs, inventory, Storage media, deployed rules/indexes, and Cloud Functions. Record existing customer data and collisions with seed IDs before designing writes. The current probe cannot provide this inventory.
2. Replace or explicitly approve illustrative product media and validate each SKU, price, MRP, stock, delivery/warranty/return claim, and brand usage. The staging seed is demonstration data.
3. Build a separate production migration with an explicit `trennex-ebbeb` target guard, a dry-run diff, selective writes, and a rollback record. **Do not run** `migrate:staging` against production or relax its staging-only guard. That script overwrites catalog documents and resets seeded inventory quantities.
4. Back up existing production Firestore data and Storage assets before the first write. A managed Firestore export requires billing and an appropriate Cloud Storage bucket; export/import operations have costs. Exported documents do not include index definitions, so retain rules and indexes separately. Confirm a restore path before migration. See [Firebase Firestore export and import](https://firebase.google.com/docs/firestore/manage-data/export-import).
5. Deploy and verify required production Firestore/Storage rules, indexes, and callable functions in a reviewed sequence. Verify production `createOrder` before enabling checkout and validate Auth/phone region, SHA configuration, function permissions, billing, quotas, and error monitoring.
6. Test a release build against production with a controlled account and minimal safe order scenario after data and services are ready. Check catalog, search, variants, cart, address, checkout, order history, logout, and assistant. Remove test artifacts and monitor errors/costs. The Android target version and signing/distribution settings also need a release review.

## Safe sequence for the next implementation issue

1. Obtain read-only production access and record the production snapshot and exact differences from staging. Keep customer accounts, carts, orders, and private data out of the catalog migration.
2. Approve final catalog content/media and prepare a production-safe migration program. It must produce a human-readable dry-run report and refuse writes by default.
3. Review the diff and backup/restore plan, then explicitly authorize the production write stage.
4. Migrate approved catalog media and documents; verify counts, references, public reads, private inventory, and checkout prerequisites. Only then plan a production release build and broader rollout.

**Decision:** production release and catalog migration are not ready. Staging remains the validation environment; this audit changes neither Firebase project.

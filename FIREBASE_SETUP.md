# TrenNex Firebase Setup

## Purpose

This configuration establishes the local Firebase foundation for the data model in
`FIREBASE_DATA_MODEL.md`. It does not deploy anything or replace the current DummyJSON integration.

The default Firebase project is `demo-trennex`, a local-only demo project ID. Firebase Emulator
Suite rejects real production access for demo projects, which protects the live TrenNex project
while rules and data migration are being developed.

## Included services

- Authentication emulator: `9099`
- Firestore emulator: `8080`
- Functions emulator: `5001`
- Storage emulator: `9199`
- Emulator UI: <http://127.0.0.1:4000>

## Install and test

From `firebase-tests`:

```powershell
npm install
npm test
```

Firebase Emulator Suite requires Java. If `java` is not on PATH on Windows, use Android Studio's
bundled JBR for the current PowerShell session before running the tests:

```powershell
$env:PATH = '<Android Studio>\jbr\bin;' + $env:PATH
npm test
```

The test command starts isolated Firestore and Storage emulators, runs the rules tests, then stops
the emulators. It never connects to the production project.

## Seed the local TrenNex catalog

The source catalog lives in `firebase-data/catalog.seed.json`. It contains editable TrenNex-owned
categories, brands, products, variants, banners, and home sections. Product images are copied from
the Android project's existing drawable assets into the local Storage emulator.

From `firebase-tests`:

```powershell
npm run seed
```

This command refuses to run unless both local emulators are available and the project ID is exactly
`demo-trennex`. It resets only the local catalog collections and `catalog/` Storage path, seeds the
data, then saves a reusable emulator snapshot under `firebase-data/emulator-snapshot`.

Verify that the exported snapshot imports correctly:

```powershell
npm run verify:seed
```

Start the normal development emulators with the saved catalog already loaded. On Windows, the
project command automatically finds Android Studio's bundled Java runtime. This command imports
the baseline but does not export temporary data when it stops:

```powershell
npm run emulators
```

The current catalog contains 10 root categories, 8 brands, 11 products, their purchasable variants
and inventory, 3 banners, 3 home sections, and public application configuration.

To run the emulators for manual Android development:

```powershell
cd firebase-tests
npx firebase emulators:start --config ../firebase.json --project demo-trennex
```

An Android emulator reaches host services at `10.0.2.2`, so the future debug-only app setup will
use `10.0.2.2:9099`, `10.0.2.2:8080`, and `10.0.2.2:9199`. A physical device needs the development
computer's LAN address instead.

## Android debug connection

Debug builds can connect Firestore and Storage to the local emulators when
`USE_FIREBASE_EMULATORS=true` is set in `local.properties`. The normal default is `false`, so the
existing Firebase authentication and user profile flows continue to use your configured project.
The host is configured by `FIREBASE_EMULATOR_HOST` (default `10.0.2.2`). Release builds never use
the emulators. The debug manifest permits cleartext traffic only so local Storage images can load.

Firebase Authentication deliberately remains connected to the real service to preserve the current
phone OTP flow. Set `USE_FIREBASE_AUTH_EMULATOR=true` in `local.properties` only when local test
users and test phone numbers are configured.

Set `USE_FIREBASE_EMULATORS=true` only when you also intend to use emulator authentication and test
accounts. Start `npm run emulators` from `firebase-tests` before launching that Android debug app.
If the emulator is unavailable, catalog reads fall back to the existing DummyJSON service.

## Security model

- Catalog reads are public only for active/published content.
- Existing unversioned DummyJSON-shaped product and category documents remain publicly readable
  during migration; versioned catalog documents must satisfy the active-content rules.
- Catalog, inventory, coupon, and order mutations require an `admin` custom claim.
- User profile, address, cart, wishlist, collection, and private asset access is owner-scoped.
- New top-level orders are read-only to their owner and must be created by trusted server code.
- Review writes are currently server/admin-only so moderation and aggregate ratings cannot be
  bypassed.
- Every path not explicitly allowed is denied.

Firebase Admin SDK and Cloud Functions bypass Firestore Security Rules. Server code must therefore
validate all inputs and enforce authorization independently.

## Temporary compatibility

The rules support the new variant-based paths used by the migrated repositories:

- `users/{uid}/cartItems`
- `users/{uid}/wishlistItems`

They also temporarily support the Android app's existing paths during rollout:

- `users/{uid}/cart`
- `users/{uid}/wishlist`
- `users/{uid}/savedAddresses`
- `users/{uid}/orders`

Legacy order creation permits only a `Pending` order with the current model's field set. Updates and
deletes remain admin-only. This compatibility block should be removed after checkout is migrated to
a callable Cloud Function and top-level `orders` documents.

During the cart and wishlist rollout, the app reads the new paths first. If no migrated documents
are available, it reads the legacy paths and attempts an automatic migration by resolving each
numeric `legacyId` to an active catalog variant. Items that cannot be resolved, or when the new
path is temporarily unavailable, remain on the legacy path so the existing UI continues to work.

Checkout now follows the same staged migration: Android creates an owner-scoped
`users/{uid}/orderRequests/{requestId}` request, but cannot create authoritative top-level orders.
The trusted `createOrder` Cloud Function is still required to validate live catalog prices, stock,
address, and payment before creating `orders/{orderId}`. Until that function is deployed, a request
may be accepted without appearing in the order-history screen; the cart is cleared only after the
request write succeeds.

## Production deployment (not performed)

Before production deployment:

1. Create a separate production alias in `.firebaserc` rather than replacing `demo-trennex`.
2. Run the full emulator rules test suite.
3. Validate every required query against the emulator and a staging Firebase project.
4. Configure admin custom claims using trusted server tooling.
5. Enable App Check for Android and server endpoints.
6. Deploy rules and indexes first to staging, then production after review.

Example deployment commands are intentionally omitted until the migration reaches the staging
approval step, preventing accidental deployment to the live project.

## Current limitations

- Emulator Suite does not reproduce every production quota or index behavior.
- Full-text product search needs a dedicated search service; Firestore is not a full-text engine.
- Android debug emulator wiring, seed data, Cloud Functions, and App Check belong to later steps.
- Production Firebase aliases and deployment credentials are deliberately not configured here.

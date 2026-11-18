# TrenNex development status

Updated 2026-11-18. This is the working status for the portfolio app roadmap.

## Issue 4: original catalog media, 2026-11-18

- Added 11 original product concept images and 3 banners in `firebase-data/assets/` (about 2.1 MB total). The old Android drawable assets remain for older hardcoded screens. The seed manifest now points at the new media.
- The local seed and staging migration scripts read the new media directory. A separate staging media publisher updates only Storage images and the product, variant, and banner image references, without resetting inventory, pricing, orders, or accounts.
- Published all 14 images to `trennex-staging-ebbeb` and refreshed the local emulator snapshot. `validate:catalog`, `verify:staging`, and `verify:seed` passed. One new staging image returned HTTP 200 with `image/jpeg`. A physical-phone staging screenshot showed the new hero and product images; `fragment_home_page.xml` now crops the wide hero to fill its card. `assembleStaging` passed and the updated APK was installed without clearing app data.
- The images are AI-generated portfolio concepts. Samsung and Lenovo assets are illustrative rather than manufacturer-approved exact SKU photos. Final commercial product photography and Figma visual comparison remain open.

## Issue 3: exact catalog variants in cart and checkout, 2026-11-18

- Product detail now uses each active Firebase variant's ID, name, color, price, and MRP. Selecting an option updates its displayed price and the Add to Cart state.
- Cart rows are keyed by account and variant ID, so two options of the same product remain separate. Checkout sends product and variant IDs for these rows. The order service remains authoritative for price and stock; legacy cart rows still use the existing numeric-ID compatibility path.
- Room version 7 to 8 migration retains old cart rows under legacy keys. `assembleStaging`, `testStagingUnitTest`, and `assembleStagingAndroidTest` passed. Four focused Room instrumentation tests passed on the connected Motorola phone using direct ADB, which kept the staging app installed and its data intact. The updated app launched with a live process. A manual variant-to-checkout UI flow still needs verification.
- No Firebase project or Cloud Function was changed for this issue. Inventory remains private under Firestore rules and is checked by the existing order function; the UI does not currently display live stock counts.

## Issue 2: account-scoped local shopping data, 2026-11-17

- Room cart items, wishlist items, and collections now use a compound key of account owner and item ID. All reads, updates, and deletes filter by owner. Guest data has its own local scope.
- Firebase Auth changes switch the visible local flows and trigger a remote cart/wishlist refresh for the newly signed-in account. Store operations capture their owner when invoked, so queued work cannot modify another account's local rows after a sign-in change. Checkout removes selected items from the account that started the order.
- The version 6 to 7 Room migration preserves previously shared rows under `__legacy_unassigned__`. No account or guest sees them automatically because their original owner cannot be proven. Signed-in cart and wishlist items can be restored from that account's Firebase data; old local-only collections remain retained but hidden until a deliberate recovery flow is designed.
- `assembleStaging` and `testStagingUnitTest` passed. Both account-isolation and version 6 to 7 migration instrumentation tests passed on a Motorola physical device using the staging build. The visible sign-out/account-switch flow still needs a UI check.
- Gradle removed the staging app after connected instrumentation tests. The staging APK was reinstalled immediately; its Room database and Firebase sign-in files were present afterward. Future device checks should run without Gradle's uninstalling test runner on the user's primary phone.
- A signed-in staging account showed one wishlist product. After sign-out, the profile switched to guest without a crash; guest wishlist showed zero items and the guest cart was empty. After signing back into the same account, the T-shirt reappeared in the wishlist. A second-account UI switch remains unverified; the Room device test covers separation of two account keys and guest data.
- The login screen's close button had no click handler. It now opens Home through the existing navigation action, allowing guest entry.

## Issue 1: staging checkout deployment, 2026-11-15

- Deployed the existing `createOrder` callable to `trennex-staging-ebbeb` in `asia-south1`. The deployment required upgrading the Cloud Functions runtime from Node 20 to Node 22 because Node 20 was no longer accepted.
- Deployed Firestore rules and indexes to staging. The staging catalog verification passed with 10 categories, 8 brands, 11 products, 15 variants and inventory records, 3 banners, 3 home sections, and 24 Storage assets.
- Confirmed the deployed callable rejects an unauthenticated request with HTTP 401 and `UNAUTHENTICATED`, rather than returning `NOT_FOUND`. The local Auth, Firestore, and Functions integration test passed its successful checkout and rejection cases; the Firestore and Storage rules test suite passed all 13 tests.
- On a physical Android device, the staging app created a ₹699 COD order for the TrenNex Essential Cotton T-Shirt. My Orders showed it as Confirmed, Order Details opened, and the cart became empty after success. The test order remains in staging order history.
- The device check exposed an Order Details bug: it hardcoded Credit Card, estimated listing price as 1.4 times the paid price, and displayed the currently selected address. Order Details now uses the saved order payment method, server-calculated listing price, delivery address, and recipient. The rebuilt staging APK was installed and these values were verified on the same test order.
- The rejected-checkout cart-preservation path is covered by code review and local function rejection tests; it was not exercised on the physical device.

## Device crash investigation, 2026-11-14

- The connected phone's crash buffer showed `SearchFragment.onViewCreated` passing a null toolbar input to `showSoftInput`. Search now waits for the toolbar view before attaching listeners or opening the keyboard. A staging device test opened Search, found a Samsung product, and opened its details without a new crash.
- The crash buffer showed `AddNewAddressFragment.onViewCreated` calling `Places.createClient` before Places was initialized. The screen now handles unavailable Places search without terminating. A staging device test opened Add New Address and its manual map screen without a new crash.
- `MainViewModel` now restarts the address observer when Firebase sign-in changes. This addresses the location bar waiting for an app restart after login; the fresh-login path still needs a device retest.
- `local.properties` and the current environment do not contain `MAPS_API_KEY`. The map appeared without tiles on the connected device. Address search and map selection cannot be fully validated until a valid Maps Platform key is configured for this build.
- Checkout now presents an order-service-unavailable message instead of a raw `NOT_FOUND` code. The staging order function and successful physical-device COD checkout were verified in Issue 1.

## What is working in this branch

- The staging catalog is verified in `trennex-staging-ebbeb`: 10 categories, 8 brands, 11 products, 15 variants and inventory records, 3 banners, 3 home sections, and 24 Storage assets.
- The staging Android build reads the owned Firebase catalog without silently switching to DummyJSON. Debug and release retain their previous fallback until production catalog migration is reviewed.
- Checkout submits a cash-on-delivery request before showing order success. Orders now reads server-created top-level orders as well as legacy user orders.
- The order function rejects unsupported payment methods, missing delivery details, and aggregate stock overuse for repeated variants.
- Cart, wishlist, and collection rows are isolated by guest or Firebase account in Room. Guest data does not show the signed-in account's wishlist, and that wishlist returns after signing back in.

## Still to complete, in order

1. Continue the full physical-device flow beyond the completed COD checkout and order-history check: OTP, login/logout, Home, product detail, location, and a rejected checkout that preserves the cart. Capture Logcat for any failure.
2. Optionally verify a second staging account through the UI. The guest → account A → logout → account A flow and the Room migration/isolation device tests passed.
3. Verify the exact-variant cart and checkout flow on a physical device, including switching between two options of the same product and a rejected checkout that preserves the cart.
4. Compare the new catalog images with the Figma visual direction. Replace illustrative branded-product concepts with exact approved photos before any commercial release.
5. Inspect remaining Figma frames visually, consolidate UI components and states, and compare phone screenshots screen by screen.
6. Add the catalog-grounded shopping assistant through a server-side Google AI integration with usage limits and tests.
7. After staging acceptance, plan production catalog migration and release separately. The production project `trennex-ebbeb` has not been changed in this update.

## Verification commands

Run `npm run validate:catalog` and `npm run verify:staging` from `firebase-tests` with staging credentials configured for the latter. Build Android with `./gradlew.bat assembleStaging testStagingUnitTest`. Run the checkout integration test using Firebase emulators for Auth, Firestore, and Functions and `node functions/integration-test.js`.

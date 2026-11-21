# TrenNex development status

Updated 2026-11-21. This is the working status for the portfolio app roadmap.

## Issue 5: Figma phone UI comparison, in progress, 2026-11-18

- The owner reports that the staging app flows checked after Issue 6 are working. The Sort frame (`2218:3167`) was visually inspected in Figma. Search now opens a branded bottom sheet with its five choices and retains Recommended and Name for existing functionality. Catalog `newArrival`, `ratingAverage`, and `ratingCount` feed real new-arrival and popularity ordering; discount and price sorting use existing fields. `assembleStaging` and `testStagingUnitTest` passed, and the updated APK was installed without clearing data. A physical-phone search confirmed Price low to high moved the ₹599 lipstick to the top. Figma blocked the subsequent Gender frame with the Starter-plan call limit, so its precise visual comparison is still open.
- Compared Figma Search start/results and location selector/Add New frames with the staging phone. The Home location bar now opens the saved-address sheet; selecting another saved address updates the bar immediately. Add New opens the existing manual map screen without a crash. The original staging address was restored after the check.
- Search result rows now use a compact photo, title, and price layout. Catalog MRP comes from the variant when available. Search requests cancel stale work and debounce suggestions; returning from product details restores the query and results, while clearing it restores recent searches. The staging phone confirmed the Samsung result opens product detail and the clear flow works. The keyboard no longer opens automatically over a restored result list.
- Restored the fuller Search result card after the compact version removed useful description and price context. Search now loads active variant records in parallel for exact MRP and discount; the staging phone showed Samsung at ₹41,999 against its real ₹45,999 MRP.
- Added working Sort (relevance, price in both directions, name) and Gender (all, men, women) controls over loaded results. Gender appears when returned products carry gender tags. The physical-phone test filtered a broad query down to the women-tagged saree and confirmed the selected filter label updates. Price-low sorting moved the ₹599 lipstick ahead of higher-priced products.
- Voice search opens Android's speech prompt and passes a recognized phrase into product search. Camera and gallery actions open the system capture/picker; on-device ML Kit text recognition and image labeling produce approximate matches against the existing catalog. Photo search loads full variant detail only for matched products. A controlled gallery test with a TrenNex Samsung image returned the Samsung product; the temporary test image was removed from the phone. Voice transcription and live camera recognition still need a person-driven test.
- `assembleStaging` passed and the staging APK was installed on the connected Motorola without clearing app data. Figma MCP reached its Starter plan call limit before the Sort/Gender frames could be inspected, so their precise visual comparison remains open. No production Firebase data or backend logic changed in this Search pass.
- Compared the Home and general product-detail frames in the TrenNex Figma file with a physical Motorola staging build. Home now uses the ten Figma category icons, Figma-sized category tiles, a taller banner, narrower search/address fields, and more compact product cards. The rebuilt device screenshot confirmed the full category labels fit.
- Kept category navigation, Firestore-backed catalog/banner loading, product taps, and the existing bottom navigation. The banner still displays image-only staging media because the current banner data exposes only image URLs; product-specific Figma promotional text must not be hardcoded over unrelated rotating images.
- Corrected product-detail variant description formatting after the device check exposed ` (, , )` on selection. The rebuilt staging APK displayed the selected 256 GB variant and its ₹47,999 price correctly.
- `assembleStaging` passed after the Home spacing adjustment. The APK was installed without clearing data, and a new physical-device screenshot confirmed the adjusted header, banner, category row, and product cards.
- Aligned Home search and location fields to the same left edge and width, matched their corners, and removed the unused "View All" text. The revised staging build passed and its physical-device screenshot showed both bars aligned with Home content loaded.
- Expanded both Home header fields to the available width, increased their vertical gap to 8dp, and tightened the notification/wishlist visual spacing without shrinking their 40dp tap views. `assembleStaging` passed; the updated staging APK and loaded Home screen were verified on the connected phone.
- The visual pass continues with remaining Home states, search/location, product-detail variants, and shared navigation. Variant-specific imagery remains a catalog-media gap: both Samsung options currently share one photo.

## Issue 4: original catalog media, 2026-11-18

- Added 11 original product concept images and 3 banners in `firebase-data/assets/` (about 2.1 MB total). The old Android drawable assets remain for older hardcoded screens. The seed manifest now points at the new media.
- The local seed and staging migration scripts read the new media directory. A separate staging media publisher updates only Storage images and the product, variant, and banner image references, without resetting inventory, pricing, orders, or accounts.
- Published all 14 images to `trennex-staging-ebbeb` and refreshed the local emulator snapshot. `validate:catalog`, `verify:staging`, and `verify:seed` passed. One new staging image returned HTTP 200 with `image/jpeg`. A physical-phone staging screenshot showed the new hero and product images; `fragment_home_page.xml` now crops the wide hero to fill its card. `assembleStaging` passed and the updated APK was installed without clearing app data.
- The images are AI-generated portfolio concepts. Samsung and Lenovo assets are illustrative rather than manufacturer-approved exact SKU photos. Final commercial product photography and Figma visual comparison remain open.

## Issue 3: exact catalog variants in cart and checkout, 2026-11-18

- Product detail now uses each active Firebase variant's ID, name, color, price, and MRP. Selecting an option updates its displayed price and the Add to Cart state.
- Cart rows are keyed by account and variant ID, so two options of the same product remain separate. Checkout sends product and variant IDs for these rows. The order service remains authoritative for price and stock; legacy cart rows still use the existing numeric-ID compatibility path.
- Room version 7 to 8 migration retains old cart rows under legacy keys. `assembleStaging`, `testStagingUnitTest`, and `assembleStagingAndroidTest` passed. Four focused Room instrumentation tests passed on the connected Motorola phone using direct ADB, which kept the staging app installed and its data intact. The updated app launched with a live process. On 18 November, the manual staging check selected both Samsung Galaxy A56 options. They appeared as separate cart rows at ₹41,999 and ₹47,999, checkout showed ₹89,998, and a COD order completed. My Orders showed two confirmed entries. The test order remains in staging history.
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

## Issue 6: catalog shopping assistant, 2026-11-20

- Added `askShoppingAssistant`, a signed-in Firebase callable in `asia-south1`. It reads active product records, selects up to five relevant products, and sends those catalog facts to Gemini 3.5 Flash-Lite through Google Cloud service identity. The phone receives no AI credentials. Recommended product IDs, prices, images, and links are taken from Firestore records, not generated by the model.
- Added daily UTC limits of 12 model questions per account and 200 across the project. A no-match question returns a catalog message without invoking the model. Assistant usage counters are stored under `assistantUsage/{date}/users/{uid}` and `assistantUsage/{date}`; question text is not stored.
- Added a Profile entry and Android shopping assistant screen with sign-in handling, product links, loading, error, and remaining-questions states. The backend helper tests cover input bounds, catalog matching, and rejecting invented product links.
- `assembleStaging`, `npm run check`, and `npm test` passed. The new callable was deployed to `trennex-staging-ebbeb` and its unauthenticated request returned HTTP 401. Its first signed-in smoke test returned HTTP 403 `SERVICE_DISABLED`, before the staging Vertex AI API was enabled. A seven-day cleanup policy was set on the staging function artifact repository. The local Firebase emulator cannot call the Cloud Run metadata service for a token. No production project was targeted.
- After the staging Vertex AI API was enabled, the signed-in smoke test returned HTTP 200 and the matching Samsung product, so the function identity had sufficient inference permission. It exposed a price-unit error in the generated prose (`4,199,900` instead of `₹41,999`). The model now receives a formatted INR price, and the response parser replaces unverified large numeric amounts with a safe catalog message. Five backend tests pass, including the price case. The corrected function was deployed to staging and returned HTTP 200 with `₹41,999.00` and the exact catalog product ID. The temporary Auth user and usage document were removed. Phone UI verification remains open.
- The updated staging APK was installed on a signed-in physical Motorola without clearing app data. Profile opened the assistant, and its first phone run exposed the keyboard covering Send; the composer now moves above the keyboard and was verified on the device. A question returned a catalog-backed Samsung answer, a ₹41,999 product card, and a remaining-questions label. Tapping the card opened the Samsung product detail without a crash. Returning with Back preserved the question, answer, card, and limit label through the screen ViewModel. The final staging APK was rebuilt and installed with TrenNex blue on Send and a readable input hint.
- Current scope is a single-question catalog assistant over the first 100 active Firestore products. Conversation survives Back and rotation in memory but is not saved across app restarts. Guest sign-in UI and a full 12-question limit run were not exercised on the physical phone; unauthenticated HTTP rejection and backend input/grounding tests passed.

## Still to complete, in order

1. Continue the full physical-device flow beyond the completed COD checkout and order-history check: OTP, login/logout, Home, product detail, location, and a rejected checkout that preserves the cart. Capture Logcat for any failure.
2. Optionally verify a second staging account through the UI. The guest → account A → logout → account A flow and the Room migration/isolation device tests passed.
3. Verify the rejected checkout path on a physical device and confirm that its cart remains intact. The successful two-variant checkout path passed.
4. Compare the new catalog images with the Figma visual direction. Replace illustrative branded-product concepts with exact approved photos before any commercial release.
5. Inspect remaining Figma frames visually, consolidate UI components and states, and compare phone screenshots screen by screen.
6. Extend assistant retrieval beyond 100 products and persist conversation history if needed after the initial staging assistant is accepted.
7. After staging acceptance, plan production catalog migration and release separately. The production project `trennex-ebbeb` has not been changed in this update.

## Verification commands

Run `npm run validate:catalog` and `npm run verify:staging` from `firebase-tests` with staging credentials configured for the latter. Build Android with `./gradlew.bat assembleStaging testStagingUnitTest`. Run the checkout integration test using Firebase emulators for Auth, Firestore, and Functions and `node functions/integration-test.js`.

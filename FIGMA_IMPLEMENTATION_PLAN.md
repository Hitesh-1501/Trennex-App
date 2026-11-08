# TrenNex Figma Implementation Plan

## Approval gate

This document is analysis only. No UI implementation, backend rewrite, API migration, database change, authentication change, or navigation change should begin until the owner explicitly approves this plan.

## Technical baseline

- Native Android app using Kotlin and XML Views.
- Single `MainActivity` with AndroidX Navigation and View Binding.
- Material Components theme with existing Figma-matching color resources.
- Room for cart, wishlist, collections, and recent search.
- Firebase Authentication and Firestore for user-owned data.
- Retrofit product API fallback.
- Google Maps/Places/location services.
- Existing debug build, unit-test task, and lint task complete successfully.

## Implementation principles

1. Implement one approved route or coherent state family at a time.
2. Preserve repository, ViewModel, Room, Firebase, Maps, Safe Args, and navigation behavior.
3. Treat Figma variants as view states unless navigation semantics require a destination.
4. Use existing adapters/layouts and extract reusable components only where equivalence is demonstrated.
5. Retrieve high-fidelity design context and a screenshot for each frame immediately before implementing it.
6. Download every required static asset through Figma and store it locally; never commit temporary Figma URLs.
7. Keep remote product/user content dynamic.
8. Validate each screen on an emulator against its exact Figma frame before moving to the next screen.

## Proposed sequence

### Phase 0 — Design access and baseline capture

- Wait for Figma MCP quota reset or increased plan access.
- Retrieve `get_design_context` plus screenshot for every implementation target, batching only closely related visible child nodes when a response is sparse.
- Capture current-app screenshots for the same routes at a 440 x 956-equivalent viewport.
- Confirm Poppins Light/ExtraBold and Inter handling.
- Confirm whether dark mode is in scope; the file does not contain a complete dark-mode screen set.
- Confirm whether only phone portrait is in scope; no tablet/desktop/landscape frames exist.

Exit condition: every first-phase screen has an accessible, node-specific Figma visual target and required assets.

### Phase 1 — Shared foundations and navigation shell

- Reconcile theme tokens without changing semantic resource values that already match.
- Establish reusable typography appearances matching the 23 Figma text styles.
- Match status/navigation bars, app bars, bottom navigation, badges, common buttons, inputs, cards, empty states, dots, and progress states.
- Keep `MainActivity` destination listeners and bottom-navigation destination IDs unchanged.

Validation: Home shell, one secondary toolbar, one form, and one list screen across small/standard/large phones.

### Phase 2 — Splash, onboarding, login, and OTP

Implementation order:

1. Splash intro states (`1744:2730`, `11:2`, `1744:2733`).
2. Onboarding pages (`16:12`, `21:57`, `21:90`).
3. Login (`2053:2452`) with all phone-field/button states.
4. OTP (`2066:2655`) with timer, resend, validation, and loading states.
5. Network/error state (`184:1181`) only after trigger behavior is agreed.

Preserve Firebase phone authentication, verification IDs, resend behavior, stored onboarding state, and navigation.

### Phase 3 — Home, search, and location entry

Implementation order:

1. Home Main (`2132:3732`) using dynamic categories, banners, and product data.
2. Welcome/location prompt states (`130:741`, `2106:2828`, `2106:3021`).
3. Search states (`2845:4685` through `2845:4863`).
4. Location selector/search states and bottom sheets.

Preserve refresh, address selection, Places search, Geocoder, saved addresses, product navigation, recent searches, and recommendations.

### Phase 4 — Category and reusable product listing

- Implement the ten root category states using the existing category destination and child fragments.
- Define one reusable catalog/listing presentation for the 49 category-detail screens.
- Parameterize category, subcategory, selection, sort/filter state, and product content.
- Add a new product-listing destination only if approved after reviewing current category behavior.

Preserve existing CategoryViewModel data and product selection. Do not create 49 independent fragments.

### Phase 5 — Product detail and specifications

- Implement general/mobile and clothing product-detail variants.
- Implement wishlist/cart/buy-now state changes.
- Implement image carousel, dots, variants, colors/sizes, reviews, price, delivery location, and specifications.
- Retain `imagePreviewFragment`; obtain or request its missing Figma target.

Preserve product fetching, selected variants, cart/wishlist writes, location flow, image preview, and product arguments.

### Phase 6 — Wishlist and collections

- Wishlist default/edit/after-delete states.
- Collection empty/list/detail states.
- Collection create/rename/delete menus and selection workflow.

Preserve Room/Firestore synchronization, selection, add-to-cart, product navigation, collection IDs, and current repository behavior.

### Phase 7 — Cart, checkout, payment, and success

- Cart empty/populated states and cart-item variants.
- Checkout/address/user-detail states.
- Payment totals and six method accordion states.
- Cart/address/payment progress state.
- Payment success/order-confirm animation sequence.

Preserve item selection, quantities, totals, addresses, user details, order creation, and navigation. Payment PIN/processing stays a visual prototype until a real provider/security design is separately approved.

### Phase 8 — Profile, account, addresses, orders, and notifications

- Signed-in and guest profile states.
- Account edit and phone OTP update states.
- Saved addresses.
- Orders, review prompt, and order detail/delivered states.
- Notification empty and populated designs.

Preserve Firebase auth/profile/address/order behavior. Review submission, delete account, coupons, help, populated notifications, and related deep links are separate functional gaps and must not be faked during a UI-only pass.

### Phase 9 — Remaining dialogs, motion, accessibility, and cleanup

- Implement only dialogs with known triggers and owned flows.
- Translate timed Figma prototype transitions into Android-native state/motion where they improve feedback.
- Add content descriptions, focus order, touch-target sizing, text scaling, contrast checks, keyboard behavior, and RTL safety.
- Remove only obsolete resources proven unused after migration.

## Screen-level workflow

For each approved screen/state family:

1. Retrieve node-specific design context and screenshot.
2. Inspect existing Fragment, XML, adapter, ViewModel, and relevant repository calls.
3. List functionality and state that cannot regress.
4. Identify reusable layouts/styles/drawables.
5. Download and verify required Figma assets.
6. Implement the smallest coherent screen/state family.
7. Build and run the app.
8. Capture emulator screenshot at the Figma baseline size.
9. Compare layout, typography, colors, images, spacing, borders, radii, hierarchy, scroll behavior, and system insets.
10. Fix visible differences, then test at additional phone widths and font scales.
11. Run targeted tests plus `assembleDebug`, unit tests, and lint.
12. Stop for review before proceeding to the next major flow if requested.

## Responsive/adaptive strategy

- Figma specifies phone portrait at 440 x 956; that is the visual baseline.
- XML layouts should use constraints, match-constraint widths, RecyclerView/ViewPager content, dimension resources, and window insets rather than fixed full-screen coordinates.
- Validate at minimum a compact phone, the Figma-equivalent phone, and a larger phone.
- Tablet, desktop, landscape, and foldable behavior cannot be matched to Figma because designs are absent. If those form factors are required, request designs or approve adaptive behavior separately.

## Functional regression checklist

- Splash/onboarding/auth decision and Firebase phone login.
- Home loading/error/refresh and product/category data.
- Search history, suggestions, results, and product navigation.
- Cart persistence/sync, quantities, selection, totals, and removal.
- Wishlist/collection persistence/sync and cart transfer.
- Product images, variants, specifications, reviews, and navigation.
- Address permission, Places, map pin, Geocoder, add/edit/delete/select.
- Checkout user/address details and order creation.
- Payment selection and success/orders navigation.
- Profile auth states, account update, saved addresses, orders, notifications.
- Back stack, process recreation, Safe Args, bottom navigation, toolbars, and cart badge.

## Risks and blockers

- Figma Starter-plan MCP rate limit currently prevents additional visual/design-context calls. Structural inventory is complete, but per-screen high-fidelity retrieval must resume before code implementation.
- The design contains 197 screen/state frames; implementing them as 197 destinations would be incorrect. State consolidation in the route map must be approved.
- Missing form-factor designs prevent faithful tablet/desktop implementation.
- Missing functional definitions prevent safe wiring of reviews, filters, delete account, help/coupons, populated notifications, camera/share, and payment PIN.
- Static asset licensing/ownership is assumed to belong to the supplied design; assets still need local export and validation.
- Figma font weights do not exactly match the bundled Android font files.
- The current working tree already contains unrelated/pre-existing IDE and configuration changes. They must remain untouched during UI implementation.

## Approval choices

Approval of this plan should confirm:

1. Phone portrait is the first target; tablet/desktop/landscape are deferred unless new frames are supplied.
2. Figma variants are consolidated into the existing 25 destinations plus, at most, one approved reusable product-listing destination.
3. UI work does not include backend/API/auth/database redesign.
4. Undefined functionality remains visually deferred or disabled until separately specified.
5. Implementation begins with Phase 1 foundations, then Phase 2 authentication, unless a different screen order is requested.


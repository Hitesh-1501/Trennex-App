# TrenNex Figma-to-Route Mapping

## Existing application navigation

The project is a single-activity Android application using AndroidX Navigation, XML layouts, View Binding, and a curved bottom navigation bar. The navigation graph currently defines 25 destinations. Figma UI states should remain states inside these destinations unless a new route is explicitly justified.

## Destination mapping

| Existing/proposed route | Figma source frames | Existing implementation | Mapping decision and preserved behavior |
|---|---|---|---|
| `splashFragment` | `1744:2730`, `11:2`, `1744:2733` | `SplashFragment`, `fragment_splash.xml` | Keep authentication/onboarding routing and process-death behavior; reproduce the staged logo/title animation inside the splash destination. |
| `onboardingFragment` | `16:12`, `21:57`, `21:90` | `OnboardingFragment`, pager adapter/page fragment | Keep pager and completion persistence; apply three Figma pages and dot/button states. |
| Network error state | `184:1181` | No dedicated route | Implement later as a reusable full-screen/error state owned by splash/auth/data screens, not a new route unless runtime behavior demands it. |
| `loginFragment` | `2053:2452`, phone input/button component variants, `2366:4101` | Firebase phone auth via `LoginFragment`/`LoginViewModel` | Preserve Firebase verification and guest/home navigation; restyle validation, loading, and error states. |
| `otpFragment` | `2066:2655`, OTP variants, timer, `2053:2586`, `2069:2944` | `OtpFragment`/`OtpViewModel` | Preserve verification ID, resend, SMS autofill, and Firebase login; map OTP box, timer, error, and progress states locally. |
| `homeFragment` | `130:741`, `2106:2828`, `2106:3021`, `2132:3732` | `HomeFragment`, `HomeViewModel`, product/category/banner adapters | Keep product loading, category selection, addresses, refresh, notification/wishlist/search navigation. Welcome/location prompts are states/overlays, not routes. |
| `searchFragment` | `2845:4685`, `2845:4722`, `2845:4755`, `2845:4777`, `2845:4926`, `2845:4863` | `SearchFragment`, recent-search Room DAO, recommendations, results adapters | Keep query, recent history, recommendations, and product navigation. Filter/sort/gender sheets are design gaps and must not be invented before interaction approval. |
| Product-listing route (proposed) | 49 catalog/detail-list frames in section `2866:3873` | Currently split between Home category selection and static category subfragments | Use one reusable data-driven product-listing destination or clearly scoped existing category child screens. Do not create 49 separate fragments. Preserve product selection and category data behavior. |
| `categoryFragment` | `1397:3127` through `1433:3402` | `CategoryFragment` plus 10 category child fragments and adapters | Keep the ten category tabs and current ViewModel data. Re-skin shared sidebar/content patterns; do not duplicate ten new route implementations. |
| `productDetailFragment` | `1521:3247`, `2688:3682`, `2694:3888`, `2991:4601`, `1524:3378`, `2703:4078`, `2703:4255` | `ProductDetailFragment`, images/colors/variants/reviews adapters | Keep product fetch, cart, wishlist, buy-now, location/address, image preview, and saved state. Model wishlist/cart variations as state. |
| `productSpecFragment` | `1337:2621` | `ProductSpecFragment`, specification adapter | Direct mapping; preserve product ID argument and dynamic specifications. |
| `imagePreviewFragment` | No clearly named standalone Figma frame | `ImagePreviewFragment`, full-image adapter | Existing function must remain. Needs a node-specific Figma reference or approval to retain current visual treatment. |
| Buy-now state | `1595:3122`, `1623:2690` | Current product/cart/checkout logic | Treat as product-detail/checkout state or bottom sheet, not automatically as a new destination. Preserve selected variant and quantity. |
| `wishlistFragment` | `1377:2889`, `2248:3518`, `2248:3563` | Wishlist Room/Firestore sync, selection, cart transfer | Keep item observation, deletion, selection, collection and add-to-cart behavior. |
| `collectionFragment` | `1382:3157`, `2248:3876`, `1384:3573` | `CollectionFragment`, local collection repository | Map empty/list/detail states while preserving create, rename, delete, and open behavior. |
| `collectionSelectionFragment` | Wishlist collection/edit states plus collection-name sheets | Existing selection destination | Preserve item-to-collection workflow; use Figma selection toolbar, cards, and creation sheet. |
| `collectionItemsFragment` | `1384:3573` and wishlist card states | Existing collection-items destination | Preserve collection ID/name arguments, item removal, cart transfer, and product navigation. |
| `cartFragment` | `1518:3002`, `1560:3312`, related cart-card/progress variants | `CartFragment`, Room/Firestore cart sync | Keep selection, quantities, removal, totals, location, and checkout navigation. Empty and populated frames are states. |
| `checkoutFragment` | `1595:2843`, `1595:3040`, address/detail sheets | `CheckoutFragment` | Keep selected items, totals, address selection, user details, and add-address navigation. |
| `paymentFragment` | `1643:2687`, `2501:4146`, payment component variants, `2423:5107`, `2514:4322` | Accordion-based payment UI | Preserve current payment-method selection and amount calculation. Current payments are simulated navigation; PIN/processing behavior requires explicit product/backend decisions later. |
| `paymentSuccessFragment` | `2562:4282`, `2579:3735`, `2562:4294`, `2579:3745`, `2584:3757`, `2594:3941`, `2594:3950`, `2594:3958` | `PaymentSuccessFragment` | Consolidate animation frames into one state sequence; preserve order-list navigation and cart cleanup behavior. |
| `profileFragment` | `1455:3009`, `1453:3005` | `ProfileFragment`, login status, sign-out | Preserve auth state, sign-out, and links to orders, addresses, account details, wishlist, notifications. |
| `accountDetailsFragment` | `1455:2897`, `2305:3697`, `1458:3210`, OTP/update sheets | Account details and Firebase phone re-verification | Preserve load/edit/save and phone verification. Delete-account design exists but business logic is not implemented; do not wire destructive behavior without separate approval. |
| `savedAddressesFragment` | `1459:3246` | Saved-address list and Firebase operations | Preserve add/edit/delete/select address flows. Map empty/list and overflow-menu states. |
| `addNewAddressFragment` | `153:1776`, `153:1787`, `153:1805`, location dialogs and address sheets | Maps, Places, Geocoder, Firebase writes | Preserve permissions, search, pin coordinates, text edits, Home/Office type, edit mode, and Safe Args. |
| `ordersFragment` | `1473:3559`, `1478:2937` | Firestore order observation/filtering | Preserve order query/filter and order-detail navigation. Review prompt is a state/action. |
| `orderDetailFragment` | `1485:2933`, `1495:3453`, order-detail sheets | Safe Args order detail | Preserve supplied order data and continue-shopping navigation. Rich delivery timeline/review interactions require validation. |
| Review flow (proposed) | `1479:2970`, `1479:3070` | No route/data write exists | Proposed dialog or nested destination only after review persistence and interaction requirements are approved. |
| `notificationFragment` | `1367:2801`, `2275:3460`, `1368:2818` | Presently an empty/explore state only | Keep existing back/explore behavior. Populated notification data and action behavior are missing and require later product/backend work. |

## Existing navigation relationships that must remain

- Splash chooses onboarding or Home based on stored/auth state.
- Onboarding proceeds to phone login.
- Login proceeds to OTP or Home.
- Home opens product detail, wishlist, notifications, search, and address creation.
- Bottom navigation switches among Home, Category, Profile, and Cart.
- Product detail opens image preview, specifications, cart, wishlist, and address selection.
- Cart proceeds through checkout, payment, success, and orders.
- Profile opens account details, addresses, orders, wishlist, notifications, and login/logout states.
- Wishlist/collections retain their product-detail and cart transfer paths.

## Missing or ambiguous route coverage

| Figma feature | Current status | Required decision before implementation |
|---|---|---|
| Product-listing drill-down for all 49 catalog frames | No single existing destination | Reuse category child UI or approve one reusable `ProductListingFragment`. |
| Filter, sort, and gender sheets | Designs exist; current Search has no matching logic | Define filters, source fields, and persistence. |
| Review submission | Design exists; no repository/API write | Define review model, permissions, moderation, and destination/sheet behavior. |
| Delete account | Design exists; no complete destructive workflow | Define Firebase Auth reauthentication and Firestore cleanup policy separately. |
| Help, coupons, terms, privacy, saved cards | Profile entries/design cues exist; routes are absent | Provide content/behavior or retain disabled/non-navigating entries. |
| Camera/share chooser | Dialog designs exist; app flow is unclear | Identify which screen launches them and why. |
| Payment PIN/processing | Visual sequence exists; payment is currently simulated | Decide provider and security architecture before functional wiring. |
| Populated notifications | Three frames exist; data source/actions are undefined | Define notification payloads and deep links later. |
| Image preview | Existing route has no clear Figma counterpart | Provide/identify its Figma node or approve current design retention. |


# TrenNex Figma Component Map

## Reuse policy

The implementation should restyle and compose existing Android components before introducing new ones. Existing adapters, repositories, ViewModels, Room entities/DAOs, Firebase listeners, navigation arguments, and screen behaviors remain the functional source of truth. Figma components define appearance and state representation, not replacements for working business logic.

## Figma-to-Android component mapping

| Figma component/group | Existing Android implementation | Planned reuse |
|---|---|---|
| `nav bar` (Home/Category/Account/Cart variants) | Curved bottom navigation in `MainActivity`, `activity_main.xml`, menu drawables, cart badge | Preserve destination IDs and badge flow; update shape, colors, icon sizing, and active state to Figma. |
| `app bar` blue/orange variants | Toolbar inflation in `MainActivity`; `toolbar_home.xml`, category/product/cart/title/search toolbars | Consolidate shared measurements/tokens while retaining destination-specific listeners and title/address updates. |
| Android status bar | Edge-to-edge/inset handling in `MainActivity` | Preserve system inset behavior; do not draw a fake status bar. Match color/icon appearance by destination. |
| Toast Message | Toast/loading/status behavior scattered across screens | Introduce a reusable visual state only when implementing its owning flow; do not replace domain error handling. |
| Phone input variants | `fragment_login.xml`, Material text input, `LoginViewModel` validation | Reuse Firebase login logic and expose enabled/typing/valid/error/disabled visual states. |
| OTP input variants | `fragment_otp.xml`, `OtpFragment`, `OtpEditText` style | Reuse OTP focus, paste/autofill, resend, and verification state. |
| OTP/verify buttons | Login/OTP Material buttons and progress views | One reusable button style/state contract; preserve click guards and loading. |
| Onboarding button and slider dots | Onboarding pager, `dot_tab.xml`, onboarding layouts | Reuse pager; map Skip/Finished and active-dot states. |
| Category bar and ten category chips | `CategoryAdapter`, `item_category_tab.xml`, Home category adapter | One data-driven chip/card component with active/default state; avoid ten duplicated implementations. |
| Product cards | `ProductAdapter`, `item_product.xml`, category-specific item layouts | Prefer one reusable product-card binding/model where layouts are visually identical; preserve click/product data. |
| Catalog headers/banners/categories | Existing category adapters and `item_*_header/banner/category.xml` layouts | Extract common visual patterns only after screenshot comparison proves equivalence. Do not collapse behavior prematurely. |
| Product image carousel/dots | `ProductImageAdapter`, `FullImageAdapter`, `product_banner_dot.xml` | Preserve dynamic images and image-preview navigation; match Figma aspect ratio and indicators. |
| Product colors and variants | `ColorVariantAdapter`, `VariantAdapter`, `item_color_variant.xml`, `item_variant.xml` | Reuse selection state and selected value; style mobile/color/size variants from component sets. |
| Reviews | `ReviewAdapter`, `item_review.xml`, Product detail data | Preserve read-only reviews. Submission components remain unimplemented until backend support is approved. |
| Add-to-cart/buy-now sheets | `AddToCartSheet`, `layout_add_to_cart.xml`, product/cart navigation | Reuse product/quantity/variant data; style sheet states and progress overlays. |
| Cart items | `CartAdapter`, `item_cart.xml`, Cart Room/Firestore sync | Preserve selection, quantity, delete, pricing, and image loading; map seven Figma variants to state. |
| Cart/address/payment progress | `toolbar_cart.xml`, `MainActivity.updateCartStep` | Direct state mapping for steps 1–3; match active/done/inactive/animated visuals. |
| Checkout product card | `CheckoutItemsAdapter`, `item_checkout_product.xml` | Preserve selected cart items and totals; restyle only. |
| Payment total/method cards | `fragment_payment.xml` and six included payment detail layouts | Preserve accordion behavior and total calculations; style each collapsed/expanded state. |
| Payment PIN/success animations | Payment success fragment and current progress views | Implement visual state sequence after payment behavior is confirmed; avoid treating Figma animation frames as separate fragments. |
| Wishlist cards/edit states | `WishlistAdapter`, `item_wishlist_product.xml`, `WishlistViewModel` | Preserve selection, delete, cart transfer, and collection actions. |
| Collection cards and empty state | `CollectionGridAdapter`, collection layouts/fragments | Preserve create/rename/delete/open behavior; map empty/list/detail states. |
| Profile rows/grid | `ProfileGridAdapter`, `item_profile_row.xml`, `item_profile_grid.xml` | Reuse current actions and auth visibility; style signed-in/guest states. |
| Saved address cards | `SaveAddressAdapter`, `SavedAddressesAdapter`, saved-address item layouts | Preserve selection, edit, delete, Home/Office icon, and Firestore IDs. |
| Location sheets and dialogs | Existing location dialogs, bottom sheets, `AddNewAddressFragment` | Preserve Maps/Places/Geocoder logic; map empty/search/filled/edit visual states. |
| Order cards/detail sections | `OrderAdapter`, `item_order.xml`, order fragments | Preserve Firestore observation/filter and Safe Args data. |
| Search recent/options/results | Search adapters and grid/list result layouts | Preserve recent-search DAO and recommendation logic; apply Figma search states. |
| Notification cards/empty state | `NotificationFragment`, `fragment_notification.xml` | Empty state can be restyled now; populated cards require a defined data model later. |

## Existing code assets to preserve

- 25 Navigation destinations and Safe Args contracts.
- 37 fragments, including ten category child fragments.
- 31 adapters.
- 16 ViewModels/UI-state owners.
- Room database for cart, wishlist, collections, and recent searches.
- Firebase Authentication and Firestore user/address/order/cart/wishlist behavior.
- Retrofit/DummyJSON fallback and product repository behavior until the separate backend migration is approved.
- Google Maps, Places, Fused Location, and Geocoder flows.
- Glide image loading and remote product imagery.
- Existing process-death/navigation fixes and toolbar/bottom-navigation listeners.

## Design-token mapping

| Semantic token | Figma | Existing Android resource | Status |
|---|---|---|---|
| Primary | `#2962FF` | `@color/colorPrimary` | Exact match |
| Accent | `#FF6D00` | `@color/colorAccent` | Exact match |
| Background | `#FFFFFF` | `@color/colorBackground` | Exact match |
| Surface | `#F7F7F7` | `@color/colorSurface` | Exact match |
| Text primary | `#212121` | `@color/textPrimary` | Exact match |
| Text secondary | `#757575` | `@color/textSecondary` | Exact match |
| Placeholder | `#BDBDBD` | `@color/textPlaceholder` | Exact match |
| Success | `#4CAF50` | `@color/colorSuccess` | Exact match |
| Error | `#E53935` | `@color/colorError` | Exact match |
| Primary button text | `#FFFFFF` | `@color/btnPrimaryText` | Exact match |
| Top app-bar gradient | Figma local paint style | `toolbar_home_gradient_color` drawable | Must compare stop positions/angle during Home implementation |
| Typography | Poppins 12–24 | Bundled Poppins 400/500/600/700 | Mostly covered; Light and ExtraBold are missing |

## Component gaps and cautions

- The Figma library uses many narrowly named, product-specific variants. Android should use data/state parameters rather than copy these into product-specific layouts.
- Some Figma components contain fixed 440 px geometry. Android implementation must preserve visual proportions while using constraints and available width.
- Several current category layouts are duplicated. Consolidation is allowed only where behavior and visuals are proven equivalent, and should be done incrementally.
- Static Figma imagery must be downloaded through Figma during the relevant screen implementation; temporary MCP asset URLs must never be committed.
- API-supplied product images must remain dynamic and must not be replaced with Figma screenshots.
- The design contains Poppins Light/ExtraBold and occasional Inter usage not fully represented by the bundled font family. This needs an explicit font decision before final typography work.


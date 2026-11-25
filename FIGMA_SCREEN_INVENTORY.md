# TrenNex Figma Screen Inventory

## Audit scope and access

- Figma file: `ooPph6ByZIvtpNWUbgxgoX` (`TrenNex`)
- Submitted node: `2132:3732` (`Home screen / Main`)
- File type: Figma Design
- Figma pages discovered: 4
- Main design page sections: 13
- Direct screen/state frames and instances on the main page: 197
- Baseline full-screen canvas: predominantly `440 x 956`
- This is an Android/mobile design. No tablet, desktop, landscape, or foldable frames were found.

The complete file hierarchy, screen names, IDs, dimensions, component-library groups, local variables, and local styles were structurally inspected. High-fidelity design context and a screenshot were retrieved for `Home screen / Main`. Section contact sheets were also retrieved and visually inspected for onboarding/auth, wishlist/collections, and account/orders. Further section screenshots were blocked by the Figma Starter-plan MCP call limit. Therefore, entries below marked as structurally inventoried must receive their own `get_design_context` and screenshot call before implementation; they have not been silently treated as visually inspected.

## Page inventory

| Page | ID | Contents | Inspection status |
|---|---|---|---|
| TrenNex App | `0:1` | Foundations, 13 UI sections, 197 screen/state frames | Full structural inventory; selected visual inspection |
| Flows | `2904:4369` | Onboarding, auth, profile, shopping, and order flow diagrams | Full structural inventory |
| Thumbnail | `1686:3056` | One composed marketing thumbnail | Full structural inventory |
| trenex components | `32:138` | Navigation, inputs, cards, buttons, progress, auth, categories, wishlist, and location components | Full structural inventory |

## Foundations

### Colors

The file defines 15 local paint styles. The Android project already contains matching core values.

| Figma style | Value |
|---|---|
| `colorPrimary` / `btnPrimary` / `btnSecondaryBorder` | `#2962FF` |
| `colorAcent` | `#FF6D00` |
| `colorBackground` | `#FFFFFF` |
| `colorSurface` | `#F7F7F7` |
| `coorTextPrimary` | `#212121` |
| `textSecondary` | `#757575` |
| `textPlaceHolder` | `#BDBDBD` |
| `colorSuccess` | `#4CAF50` |
| `colorErr` | `#E53935` |
| `btnPrimaryText` | `#FFFFFF` |
| `top bar gradient color` | Dark navy to translucent blue-gray gradient |
| `fashion tooolbar color` | Orange gradient |
| `color cards` | Indigo-to-dark-navy gradient |

### Typography

- Font family: Poppins.
- Local text styles: 23.
- Heading styles: 14, 16, 20, and 24 px in Bold and SemiBold.
- Body styles: 12–16 px in Regular, Medium, and Light.
- Some category labels use Inter Regular 10 px inside the Home frame.
- Figma also references Poppins ExtraBold and Light in individual layers. The repository currently bundles Poppins Regular, Medium, SemiBold, and Bold only; Light/ExtraBold require confirmation or substitution approval before implementation.

### Variables and other styles

- One local variable collection exists with one string variable: `phoneNumber`.
- No local effect styles were found.
- No local grid styles were found.
- The visual palette and typography are mostly represented as legacy local styles rather than semantic Figma variables.

## Complete main-page screen/state inventory

### Sports screen content — 1

- `1278:5703` — sport screen content (`1061 x 1214`)

### Animation components — 2

- `2106:2505` — Toast Message
- `2307:3852` — verify animation

### Progress/loading states — 23

- `2053:2586` — progress bar/ Get otp
- `2148:4076` — progress bar / update location
- `2069:2944` — progress bar / verify otp
- `2152:3003` — progress bar / search clothes
- `2218:3267` — progress bar / woman
- `2213:3126` — progress bar / T-shirts
- `2234:3070` — progress bar/ beauty / face wash
- `2218:3292` — progress bar/ kids
- `2694:3883` — progressbar mobile / cart
- `2694:3876` — progressbar mobile / wishlist
- `2700:4065` — progressbar mobile / buy now
- `2703:4208` — progressbar wishlist tshirt
- `2705:4390` — progressbar buy now tshirt
- `2703:4245` — progressbar cart tshirt
- `2264:2911` — wishlist / delete / Move
- `2264:3270` — progressbar/ new collection
- `2323:3387` — progress bar profile / logout
- `2328:3774` — progress bar save details
- `2305:3810` — progress bar/ update number
- `2321:3374` — progressbar / delete Acc
- `2857:5227` — progress bar phone pay payment
- `2857:5225` — progressbar payment options
- `2560:4219` — progress bar process payment

### Dialogs, overlays, sheets, and menus — 40

- `153:1675` — location dialog
- `184:1166` — camera permission
- `184:1175` — camera permission deny
- `1459:3355` — delete location
- `153:1763` — device location
- `2123:3254` — device location
- `2218:3167` — sort screen
- `2218:3152` — Gender screen
- `2125:3443` — location details/default location
- `1459:3305` — update location
- `2132:3994` — location details/location fill variant
- `1595:3059` — complete address details
- `2218:3319` — Gender screen variant
- `2218:3300` — Gender screen variant
- `2218:3233` — Gender screen variant
- `2218:3252` — sort screen variant
- `2218:3274` — Gender screen variant
- `1360:4795` — share chooser
- `2693:3850` — wishlist bar
- `2703:4215` — wishlist bar clothes
- `2698:4043` — Add to cart
- `2703:4230` — Add to cart clothes
- `1544:3129` — trennex add to cart
- `1386:3634` — delete from wishlist
- `1386:3614` — collection name mobile screen
- `1384:3565` — collection name mobile screen variant
- `2305:3808` — verify otp
- `1544:3147` — trennex add to cart1
- `1485:3071` — Help
- `1491:3106` — Delivery address
- `1491:3128` — Name and phone number
- `1591:2687` — remove 1 item
- `1495:3149` — order details sheet
- `1495:3759` — order details sheet variant
- `2366:4101` — login err page
- `1562:3489` — quantity
- `1479:3074` — filter
- `1479:3070` — review submit
- `153:1399` — edit and delete menu
- `1384:3599` — collection menu

### Onboarding and authentication — 9

- `184:1181` — Network connection
- `1744:2730` — intro start
- `11:2` — intro page/ logo
- `1744:2733` — intro page/ Title
- `16:12` — onboarding 1
- `21:57` — onboarding 2
- `21:90` — onboarding 3
- `2053:2452` — phone screen
- `2066:2655` — otp screen

### Home and search — 10

- `130:741` — Home Screen / welcome
- `2106:2828` — Home screen / ask location
- `2106:3021` — Home screen / location setup
- `2132:3732` — Home screen / Main
- `2845:4685` — search bar / start
- `2845:4722` — search bar / del recent searches
- `2845:4755` — search bar / search cloth
- `2845:4777` — search bar / clothes
- `2845:4926` — search bar / mobile
- `2845:4863` — search bar / laptop

### Location flow — 8

- `1595:3040` — Buy Now / select Delivery Address
- `1484:2905` — location dialog/ empty
- `2119:3226` — location dialog / search bar
- `153:1482` — location dialog / search complete
- `153:1578` — location dialog / Main
- `153:1776` — location dialog / add new
- `153:1787` — location dialog / add new / location page
- `153:1805` — location dialog / edit / update location

### Product catalog and category-detail screens — 49

- `2181:3238` — fashion screen Men
- `224:968` — fashion screen women
- `228:835` — fashion kids / girls
- `232:1231` — fashion kids / boys
- `232:1446` — fashion kids / teens
- `232:1638` — fashion kids / infants
- `235:1834` — Tshirt screen
- `240:2152` — Saree Screen
- `240:2429` — kids / girl / clothing set
- `240:2591` — kids / girl / clothing set / selected
- `240:2710` — kids / boys / sweaters
- `248:1050` — kids / boys / sweaters / selected
- `248:1522` — Mobile Screen
- `265:1162` — Mobile iphone Screen
- `277:1149` — Mobile iphone all variants
- `285:1381` — Electronics screen
- `306:1548` — Electronics laptop screen
- `317:1466` — Electronics Accessories screen
- `317:1816` — Electronics Gaming Accessories screen
- `333:1472` — intel i5 laptop screens
- `333:1627` — Electronics gaming laptop screen
- `339:1117` — Appliance Screen
- `343:2352` — Appliance television Screen
- `351:1575` — Samsung tv screen
- `373:1132` — beauty screen
- `376:1839` — Face wash screen
- `380:1278` — Home Screen
- `391:2224` — Home Screen1
- `393:2478` — cookware
- `465:1353` — furniture screen
- `571:1428` — sofas screen
- `2234:2961` — Face wash screen variant
- `596:1384` — toys screen
- `793:1420` — toys screen 1
- `809:1584` — toys screen 2
- `809:1853` — toys screen 3
- `814:1524` — toys screen 4
- `815:1742` — toys screen 5
- `825:1962` — toys screen 6
- `825:2181` — toys screen 7
- `891:2457` — sports screen
- `1278:2327` — home gym screen 1
- `1278:2664` — Badminton screen1
- `1278:3334` — home gym and badminton screen
- `1278:4852` — watches and badminton
- `1278:3751` — fitness monitor screen 1
- `1278:4086` — cricket screen 1
- `1278:4405` — fitness and cricket screen
- `1278:5208` — cricket and home gym

### Wishlist and collections — 6

- `1377:2889` — wishlist
- `2248:3518` — wishlist edit
- `2248:3563` — wishlist after delete
- `1382:3157` — collections empty page
- `2248:3876` — wishlist collection
- `1384:3573` — collection page 1

### Root category tabs — 10

- `1397:3127` — categories top picks
- `1399:3260` — categories fashion
- `1405:3540` — categories Appliances
- `1411:2825` — categories mobiles
- `1415:2994` — categories Electronics
- `1417:3243` — categories home
- `1423:3510` — categories Beauty
- `1431:2857` — categories furniture
- `1431:3188` — categories toys, books
- `1433:3402` — categories sport

### Account and orders — 11

- `1455:3009` — Account (signed in)
- `1453:3005` — Account with no login
- `1455:2897` — Account / manage account state
- `2305:3697` — Account / phone update state
- `1458:3210` — Account / delete account state
- `1459:3246` — saved addresses
- `1473:3559` — orders section default
- `1478:2937` — orders section review
- `1479:2970` — review screen
- `1485:2933` — order details
- `1495:3453` — delivered page

### Cart, checkout, and payment — 15

- `1518:3002` — Add to cart section empty
- `1560:3312` — Add to cart section
- `1595:2843` — Add to cart section (Address)
- `1643:2687` — Add to cart section (payment)
- `2501:4146` — Add to cart section (payment) variant
- `2423:5107` — payment_processing
- `2514:4322` — pin screen
- `2562:4282` — payment successful green animation
- `2579:3735` — payment successful green animation1
- `2562:4294` — payment successful screen start
- `2579:3745` — payment successful screen
- `2584:3757` — redirect page
- `2594:3941` — TrenNex order confirm page start
- `2594:3950` — TrenNex order confirm page
- `2594:3958` — orders loading page

### Product detail and notifications — 13

- `1521:3247` — product screen
- `2688:3682` — product screen / wishlist
- `2694:3888` — product screen / cart
- `2991:4601` — product screen / navigate back order summary
- `1337:2621` — Product specification screen
- `1524:3378` — tshirt product screen
- `2703:4078` — tshirt product screen / wishlist
- `2703:4255` — tshirt product screen / cart
- `1595:3122` — product buy now
- `1623:2690` — product buy now 1
- `1367:2801` — Notification page state 1
- `2275:3460` — Notification page state 2
- `1368:2818` — Notification page state 3

## Flow-page inventory

- Onboarding Flow: two onboarding frames, two splash frames, and connectors.
- Auth Flow: phone, OTP, and Home Main frames.
- Profile Flow: Account, Orders, and Order Details frames.
- Shopping Flow: Home, product detail, buy-now, payment, and success frames.
- Order Flow: cart, address, payment variants, and success frame.

## Component-library inventory

The component page contains approximately 63 component sets and 13 standalone components organized into these groups:

- Navigation: bottom navigation, app bar, Android status bar, toast.
- Inputs: phone-field and OTP-field state sets.
- Product cards: category chips, product option/variant selectors, colors, sizes, and reviews.
- Cart cards: cart-item variants.
- Order card: destination and delivered-order cards.
- Payment cards: totals, saved method, UPI, card, EMI, net banking, COD, and PIN variants.
- Buttons: OTP/verify, onboarding state, and place-order variants.
- Branding: logo, headline icon, small logo, and title.
- Dots: onboarding, banner, and product-slider indicators.
- Progress: generic, cart/address/payment step, and animated progress sets.
- Animated components: verification, countdown timer, and PIN indicators.
- Login cards: phone, OTP, and profile OTP variants.
- Search controls: sort and gender-filter sheets.
- Category items: ten category state sets and category bar.
- Wishlist cards: edit-selection states, product cards, and collection states.
- Location dialog: default and filled address-detail components.

## Known access and design limitations

- Figma MCP calls were rate-limited by the authenticated Starter plan with a View seat after the structural inventory and four visual captures.
- Every frame is inventoried by ID and name, but not every one has a high-fidelity screenshot/design-context payload yet.
- No tablet, desktop, landscape, or foldable design exists in the file.
- No complete dark-mode screen set was found; only a palette reference exists.
- Several frames are duplicates or UI states, not separate destinations.
- Some names contain spelling inconsistencies (`Acent`, `coorTextPrimary`, `wisghlist`, `ouantity`, etc.); IDs are the authoritative references.
- Some Figma frames show functionality not implemented in the current app: camera/share flows, filters, gender selection, help, coupons, delete account, review submission, payment PIN/processing, and richer notification states.


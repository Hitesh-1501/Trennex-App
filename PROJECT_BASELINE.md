# TrenNex Development Baseline

Recorded on 2026-11-08 before the Firebase catalog/backend expansion.

## Git baseline

- Development branch: `codex/trennex-development`
- Starting commit: `15aff67`
- The branch was created while preserving all existing uncommitted files and staged IDE configuration.
- No existing user changes were discarded or overwritten.

## Build environment

- Android Gradle Plugin: 8.13.2
- Gradle wrapper: 8.13
- Kotlin: 2.2.21
- Compile/target SDK: 36
- Minimum SDK: 29
- Java runtime used for verification: Android Studio bundled JBR 21

## Verification result

Command:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Result:

- Build: successful
- Debug APK: generated
- APK size: 63,486,041 bytes
- Unit tests: 1 passed, 0 failed, 0 skipped
- Lint: completed with 0 errors and 1,102 warnings
- Lint report: `app/build/reports/lint-results-debug.html`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

The only JVM test is the generated `ExampleUnitTest`, so the project currently has no meaningful behavioral unit-test coverage.

## Clean-build limitation

The initial `clean` task could not delete one Gradle lint-cache JAR because Windows reported that the file was open. The subsequent full compile, APK assembly, unit-test, and lint tasks all executed successfully. No force deletion was attempted.

## Largest lint categories

| Count | Category |
|---:|---|
| 308 | HardcodedText |
| 297 | IconLocation |
| 167 | ContentDescription |
| 80 | SetTextI18n |
| 56 | UnusedResources |
| 35 | Typos |
| 26 | Overdraw |
| 25 | GradleDependency |
| 19 | Autofill |
| 14 | UseCompoundDrawables |
| 13 | SmallSp |
| 12 | LabelFor |
| 8 | DefaultLocale |
| 7 | UseKtx |
| 6 | InflateParams |

## Confirmed functional architecture

- AndroidX Navigation with 25 destinations.
- XML Views and View Binding.
- Room persistence for cart, wishlist, collections, and recent search.
- Firebase Authentication for phone login.
- Cloud Firestore for user profile, addresses, orders, cart, wishlist, and optional catalog data.
- Retrofit/DummyJSON catalog fallback.
- Google Maps, Places, Fused Location Provider, and Geocoder address flow.
- Glide remote image loading.
- StateFlow/coroutine-based ViewModels.

## Known technical risks to address incrementally

- Product data silently falls back to DummyJSON instead of using a fully owned catalog.
- Tests are templates rather than application behavior tests.
- Room uses destructive migration fallback.
- Several Android and Google APIs in active code are deprecated.
- Large Home, Product, Cart, and Checkout fragments duplicate address/location behavior.
- Some adapters and fragments contain unsafe null assertions.
- Logging and exception handling are inconsistent.
- Lint identifies substantial accessibility, localization, and maintainability debt.
- Notification functionality is mostly an empty-state screen.
- Payment flow is a UI simulation, not a secured payment-provider integration.

## Baseline rule

Every implementation stage should keep these minimum checks green:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

New backend/catalog work should add real unit and Firebase Emulator tests rather than relying on the template test.


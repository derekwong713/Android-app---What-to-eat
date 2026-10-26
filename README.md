# 食咩好？ What To Eat?

A native Android MVP for choosing a nearby restaurant in Hong Kong. Kotlin, Jetpack Compose, Material 3, single Activity, Navigation Compose, Hilt, Room, DataStore and StateFlow. No account, analytics SDK, embedded map, or backend.

## Open and run

1. In Android Studio choose **Open** and select this folder (the folder containing `settings.gradle.kts`).
2. Use **JDK 17 or 21** for Gradle (Settings → Build Tools → Gradle). Android Studio's bundled JDK 25 is too new for this pinned Gradle baseline.
3. Install Android SDK **36**, Build Tools **35.0.0**, platform tools, and a Google APIs emulator image using SDK Manager. Accept SDK licenses. `local.properties` is machine-specific and ignored.
4. Let Gradle sync. In **Build Variants**, select **demoDebug** for the `app` module.
5. Select an emulator or connected device, then Run `app`. Demo does not need an API key or location permission. Choose a district and tap 「搵附近餐廳」, then 「幫我揀一間」.

Traditional Chinese / Hong Kong copy is the default. An English resource catalog is included; Android selects it for English device locales. Distances, ratings and dates use the device locale. The theme follows the device light/dark setting. The app's Reduce motion preference and system animator setting both suppress wheel motion.

## Commands

Windows (PowerShell), after setting `JAVA_HOME` to JDK 17/21:

```powershell
.\gradlew.bat :app:assembleDemoDebug
.\gradlew.bat :app:assembleLiveDebug
.\gradlew.bat :app:testDemoDebugUnitTest
.\gradlew.bat :app:lintDemoDebug :app:lintLiveDebug
.\gradlew.bat :app:connectedDemoDebugAndroidTest
```

macOS/Linux: use `sh ./gradlew` with the same tasks. The Gradle Wrapper JAR and both launchers are included. Android Studio opens this root directly; no generation step is needed.

Demo APK output: `app/build/outputs/apk/demo/debug/app-demo-debug.apk`. Debug APKs use Gradle's development signing key. Release signing is intentionally not configured.

## Pinned compatibility baseline

Every library/plugin version is in `gradle/libs.versions.toml`; no dynamic versions. This deliberately uses a stable AGP 8 / API 36 baseline rather than requiring a migration to AGP 9's built-in Kotlin or Places 6's removed methods.

| Component | Pinned version |
| --- | --- |
| AGP / Gradle | 8.13.2 / 8.13 |
| Kotlin and Compose compiler plugin | 2.2.21 |
| Compose stable BOM | 2025.10.01 |
| Activity / Core | 1.11.0 / 1.17.0 |
| Lifecycle / Navigation Compose | 2.9.4 / 2.9.5 |
| Hilt (kapt) / Room (kapt) | 2.57.2 / 2.8.4 |
| DataStore / Coroutines | 1.2.1 / 1.10.2 |
| Places Android / Fused Location | 4.4.1 / 21.3.0 |
| Material Views (Places dependency) | 1.13.0 |
| Minimum / compile / target SDK | 26 / 36 / 36 |

Java/Kotlin bytecode targets 17. minSdk 26 allows `java.time` and the system location-services APIs without legacy compatibility code. API 36 is within AGP 8.13's documented supported range (up to 36.1). Newer stable versions exist; these are pinned supported releases, not a claim to be the newest releases. Re-check target SDK / Play submission requirements at release time.

Official references checked while implementing:

- [AGP 8.13 compatibility and 8.13.2](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) and [Kotlin 2 compiler plugin](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
- [Kotlin Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Hilt setup](https://dagger.dev/hilt/gradle-setup), [Room](https://developer.android.com/jetpack/androidx/releases/room), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore), [Navigation](https://developer.android.com/jetpack/androidx/releases/navigation), [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle)
- [Places release notes](https://developers.google.com/maps/documentation/places/android-sdk/release-notes), [Nearby Search New](https://developers.google.com/maps/documentation/places/android-sdk/nearby-search), [supported types](https://developers.google.com/maps/documentation/places/android-sdk/place-types), [Place API](https://developers.google.com/maps/documentation/places/android-sdk/reference/com/google/android/libraries/places/api/model/Place)
- [Places policies and attribution](https://developers.google.com/maps/documentation/places/android-sdk/policies), [Maps URLs directions](https://developers.google.com/maps/documentation/urls/guide#directions-action)
- [Android foreground location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime), [Play services setup](https://developers.google.com/android/guides/setup)

## Demo fixtures

Every restaurant and evidence statement is fictional, visibly marked on every app screen, including the result dialog. Demo never creates a Places client or calls Places. Device location is optional; manually selected district centers work offline.

- Mong Kok / Central: mixed cuisines, prices, unknown data, conflicting allergens and a closed business.
- Causeway Bay: up to 20 restaurants, useful for numbered-wheel testing.
- Sha Tin: one candidate, useful for one-candidate and exhausted-round flows.
- Tsuen Wan: zero results.
- Peanut avoidance in Warning mode excludes fictional contains/cross-contact evidence but keeps unknowns after acknowledgment.
- Sha Tin + milk avoidance + Conservative mode produces zero eligible candidates.
- Fictional accommodation evidence is explicitly labeled and never presented as an allergen-free guarantee. Narrow dish-only evidence is Unknown for a restaurant-wide selection. Evidence older than 90 days or future-dated is Unknown.

Demo navigation opens sample coordinates in external Maps/browser, with an explicit fictional-data warning. Fictional IDs are never sent as real Google Place IDs.

## Live setup

Copy `secrets.properties.example` to ignored `secrets.properties`, or inject the same environment variables in CI:

```properties
PLACES_API_KEY_DEBUG=
PLACES_API_KEY_RELEASE=
PRIVACY_POLICY_URL=
TERMS_URL=
```

Enable billing and **Places API (New)** in Google Cloud. Restrict Android keys to `hk.whatoeat` and the correct debug/release SHA-1 signing certificates, and to the required API. Use different debug and release keys/projects. An APK-embedded key is extractable; restrictions, monitoring and rotation are essential. Do not paste keys into tracked files or logs.

Select **liveDebug**. Missing keys and provider configuration failures surface as errors; live never falls back to demo. Search requests use explicit fields, the `restaurant` type, a circular restriction, DISTANCE ranking and a maximum of 20. Cuisine tags come only from an explicit provider-type mapping. Numeric price levels 0 (free) and 1 map to Budget; 2 Moderate, 3 Higher, 4 Premium, everything else Unknown. No invented HKD price estimates.

No request retries run automatically. Refresh is explicit, so configuration/quota errors cannot trigger retry storms. Ordinary redraws and mode changes use the existing candidate snapshot and make no search calls. Place details load only when an item is opened; there is no bulk hydration of saved lists. Provider text lives in memory, with only IDs and user interactions persisted. Backups are disabled for the complete MVP, including sensitive preferences and prepared sessions.

**The live allergen repository returns Unknown for every restaurant.** Google Places is not treated as a reliable allergen source. Conservative mode with any avoidance item therefore has no live candidates. This is explained in the UI, without relaxing the user's policy.

## Privacy, terms and attribution

The app requests foreground coarse+fine location together only after Use my location. Approximate permission is accepted. A fix includes timestamp and accuracy. A fix older than five minutes triggers a refresh; if only the old fix remains, the user must explicitly opt in or select a district. No background permission exists. Provider failures are shown distinctly from successful empty results.

Google Maps text attribution appears in compact provider-content containers using the required spelling/system sans serif. Returned third-party HTML attribution keeps links using Android's HTML renderer. Photos and full reviews are excluded. Before production, review attribution against current policy on real responses and all screen sizes. Settings links to Google's terms/privacy and to configured publisher URLs. Empty publisher URLs are labeled as release blockers. No public production URLs are invented.

See [architecture.md](architecture.md), [acceptance-checklist.md](acceptance-checklist.md), [release-checklist.md](release-checklist.md), and [verification.md](verification.md) for behavior, verification evidence and outstanding release work.

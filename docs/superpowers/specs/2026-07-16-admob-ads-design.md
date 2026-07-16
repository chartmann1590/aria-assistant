# AdMob Banner + Interstitial Ads — Design

## Goal

Add Google AdMob monetization to Aria: a banner ad on the main screen and periodic
interstitial ads during use, visible only to non-Premium users. Ad unit IDs and the
AdMob App ID are supplied via `local.properties` locally and GitHub Actions secrets
in CI, following the existing pattern used for the GitHub feedback-reporter
credentials (`app/build.gradle.kts` `readProp()`).

## Decisions (confirmed with user)

- **Ad-free Premium**: ads are hidden entirely for Premium subscribers. This becomes
  an additional Premium perk. The README's current "Aria contains no advertising SDK"
  claim will be corrected to describe the free-tier ad behavior.
- **Banner placement**: bottom of `MainScreen`, directly above the floating input bar.
- **Interstitial trigger**: shown after every 4th user interaction (text message sent
  or voice query started), with a minimum 60-second cooldown between interstitials so
  it never fires twice in a row for rapid-fire messages.

## Components

### 1. Gradle wiring
- `gradle/libs.versions.toml`: add `playServicesAds` version + `play-services-ads` library.
- `app/build.gradle.kts`:
  - `implementation(libs.play.services.ads)`
  - `readProp("admob.app.id")` → `manifestPlaceholders["adMobAppId"]`
  - `readProp("admob.banner.id")` / `readProp("admob.interstitial.id")` →
    `buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", ...)` /
    `ADMOB_INTERSTITIAL_AD_UNIT_ID`
  - Falls back to Google's public test ad unit IDs when the properties are blank, so a
    clean checkout without `local.properties` still builds and shows test ads instead
    of crashing or shipping blank IDs.

### 2. Manifest
- `AndroidManifest.xml` gets the required
  `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="${adMobAppId}" />`
  inside `<application>`.

### 3. `AdManager` (new, `ads/AdManager.kt`, `@Singleton`)
- Wraps `MobileAds.initialize()`, interstitial loading, and interstitial display logic.
- `recordInteraction()`: increments a counter; every 4th call (and only if the
  cooldown has elapsed and an interstitial is loaded and the user isn't Premium)
  emits a one-shot `SharedFlow<Unit>` event for the UI layer to act on.
- `showInterstitial(activity)`: shows the currently loaded interstitial (if any) and
  immediately requests the next one.
- Depends on `FeatureGate`/`BillingManager.isPremium` to gate everything.

### 4. `BannerAdView` (new, `presentation/component/BannerAdView.kt`)
- Compose `AndroidView` wrapping a standard AdMob `AdView`, loads
  `BuildConfig.ADMOB_BANNER_AD_UNIT_ID`, adaptive banner sized to screen width.
- `MainScreen` renders it conditionally (`if (!isPremium)`) directly above the
  floating input bar, matching the existing padding/spacing conventions.

### 5. `MainViewModel` wiring
- Inject `FeatureGate` (exposes `isPremium: StateFlow<Boolean>`, already used
  elsewhere) and `AdManager`.
- Expose `isPremium` to `MainScreen` for the banner.
- Call `adManager.recordInteraction()` at the top of `sendMessage()` and at the start
  of a new voice session in `triggerListening()`.
- Expose `adManager.interstitialTrigger` (or re-expose directly) so `MainScreen`
  collects it and calls `adManager.showInterstitial(activity)` using
  `LocalContext.current` cast to `Activity`.

### 6. `AriaApplication`
- Call `MobileAds.initialize(this)` in `onCreate()` alongside the existing
  notification-channel setup.

## CI / Secrets

- `.github/workflows/build.yml` passes three new `-P` Gradle properties from repo
  secrets: `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`, mirroring the
  existing `GH_API_TOKEN` pattern.
- The three GitHub repository secrets are set via `gh secret set` (requires explicit
  user confirmation before running, since it mutates the remote repo).
- `local.properties` gets `admob.app.id`, `admob.banner.id`, `admob.interstitial.id`
  set to the real values for local builds. It is already gitignored — verified, not
  changed.

## Out of scope

- No ad-request consent/UMP flow (no GDPR/CCPA consent SDK) — out of scope unless the
  user asks; Play Console publishing will still require configuring this separately
  before a production release, but it's not part of this change.
- No rewarded ads, no native ads — only banner + interstitial as requested.
- No server-side ad configuration/remote toggling — ad unit IDs are build-time only.

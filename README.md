# CurrencyManual

**App name:** Currency Converter  
**Version:** 1.0.3  
**Android versionCode:** 4  
**Archive:** `CurrencyConverter-v1.0.3-source-for-git.zip`

Android currency converter, Java 17, minSdk 23, compileSdk 35.

## Version tracking

The version is defined in `app/build.gradle`, recorded in `VERSION.txt`, and displayed in Settings. Tap the version row to open the About dialog. Every source archive uses the same version in its filename. Increment `versionCode` for every published build and update `versionName`, `VERSION.txt`, and `CHANGELOG.md` together.

## Features
- Dark saradmin interface and built-in numeric keypad. The clear key (✕) is part of the keypad and uses the same key styling as digits.
- Session-only amount input. The amount is cleared when the app goes to background.
- Two to six selected currencies; base currency can be changed without changing the entered numeric amount.
- Currency factors are normalized internally to USD only as a calculation reference. USD is not forced as the user's base currency.
- Automatic rate refresh at the configured interval (15 minutes by default; 30 minutes, 1, 6, 12 or 24 hours are available).
- Provider 1: Frankfurter API (`api.frankfurter.dev`). Provider 2: ExchangeRate-API Open Access (`open.er-api.com`). Provider 1 takes precedence; provider 2 fills missing currencies and is used if provider 1 is unavailable.
- All fetched rates are cached locally. If both services are unavailable, the last saved values remain available. Manually edited rates are not overwritten by automatic updates.
- Source badges: ¹ Frankfurter, ² ExchangeRate-API, blue for manual or fallback data.
- Add currencies from the local catalog. A cached API rate is used automatically when available; otherwise the app asks for a manual rate. Existing currency rates can be edited in Settings.
- Remove additional currencies (at least two currencies must remain).

## Build
GitHub Actions runs:

```bash
gradle --no-daemon clean testDebugUnitTest assembleDebug
```

The signed release APK is uploaded as the `CurrencyConverter-v1.0.3-release-apk` workflow artifact.

## Rate semantics
Frankfurter uses central-bank reference data and its publication schedule. ExchangeRate-API Open Access updates daily. The app's refresh interval controls local polling, not the publication frequency of either provider. Rates are indicative and may differ from bank/card exchange rates.

## Tests
JUnit tests cover conversion arithmetic, cross rates, validation, provider priority, fallback merging and missing-rate handling. Network availability and live provider responses are not assumed by unit tests.

## Release signing (1.0.2+)

Release APKs are signed with a persistent private keystore supplied through GitHub Actions Secrets. Follow `BUILD_FROM_PHONE.md` for the one-time setup. Do not commit the keystore or passwords. CI verifies the APK signature and package/version metadata before uploading the release artifact.

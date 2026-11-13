# Currency Converter

**App name:** Currency Converter  
**Version:** 1.0.16  
**Source archive:** `CurrencyConverter-v1.0.16-source-for-git.zip`

Android currency converter, Java 17, minSdk 23, compileSdk 35.

## Versioning

`VERSION.txt` is the single source of truth for the app version. For every release, change only the `Version` value there. Gradle automatically derives Android's mandatory internal `versionCode` from the semantic version; it is not maintained or displayed separately. The app UI and About dialog show only the app version. The workflow artifact name and Termux APK filename are also generated from this same version.

## Features
- Dark saradmin interface and built-in numeric keypad. The bottom keypad row contains only decimal point, zero and backspace.
- Session-only amount input. The amount is cleared when the app goes to background.
- Two to six selected currencies; base currency can be changed without changing the entered numeric amount.
- Currency factors are normalized internally to USD only as a calculation reference. USD is not forced as the user's base currency.
- Automatic rate refresh at the configured interval (15 minutes by default; 30 minutes, 1, 6, 12 or 24 hours are available).
- Provider 1: Frankfurter API (`api.frankfurter.dev`). Provider 2: ExchangeRate-API Open Access (`open.er-api.com`). Provider 1 takes precedence; provider 2 fills missing currencies and is used if provider 1 is unavailable.
- All fetched rates are cached locally. If both services are unavailable, the last saved values remain available. Manually edited rates are not overwritten by automatic updates.
- Source badges: 1 Frankfurter, 2 ExchangeRate-API, blue for manual or fallback data.
- Per-currency Keep this rate option protects a manually set rate from API updates.
- Settings are edited as a draft: Save persists rate edits, currency additions/removals and update interval; Cancel discards all changes made since the settings screen was opened.
- Result currencies can be reordered by dragging; the order persists across app restarts.
- Amounts and rates use space-separated thousands grouping and two decimal places in results.

## Build

GitHub Actions runs unit tests and builds a signed release APK. The artifact name is generated from `VERSION.txt`.

## Build and install from Termux

After pushing a commit to `main`, run `bash scripts/build-install.sh` from the project root. The script waits for the GitHub Actions run matching the pushed commit, checks the result, downloads the artifact, verifies the APK archive, and renames it using the app name and version. Install manually through ES File Explorer on this Huawei device.

# Currency Converter

**App name:** Currency Converter  
**Version:** 1.1.2  
**Source archive:** `CurrencyConverter-v1.1.2-source-for-git.zip`

Android currency converter, Java 17, minSdk 23, compileSdk 35.

## Versioning

`VERSION.txt` is the single source of truth for the app version. For every release, change only the `Version` value there. Gradle derives Android's mandatory internal `versionCode` automatically. GitHub Actions verifies that this README version matches `VERSION.txt`. The app UI, About dialog, release artifact and downloaded APK use the same version.

## Features

- Dark saradmin interface with the blue/red interlocking C brand mark and circular exchange arrows, plus a built-in numeric keypad. The bottom keypad row contains only decimal point, zero and backspace.
- Session-only amount input. The amount is cleared when the app goes to background.
- Two to six selected currencies; base currency can be changed without changing the entered numeric amount.
- Exchange rates are stored as independent directed currency-pair quotes. The selected base currency is only the view used by the converter; changing it does not rewrite pair rates or their source metadata.
- Rates refresh once whenever the app is opened. `Update rates now` forces an immediate refresh of all available unpinned currency pairs. There is no periodic timer or update-interval setting.
- Provider 1: Frankfurter API (`api.frankfurter.dev`). Provider 2: ExchangeRate-API Open Access (`open.er-api.com`). Provider 1 takes precedence; provider 2 fills missing currencies and is used if provider 1 is unavailable.
- Fetched rates are cached locally. If providers are unavailable, the last saved rates remain available.
- Source badges: 1 Frankfurter, 2 ExchangeRate-API (including cross-rates calculated from its USD quotes), blue for manual data. Source belongs to the currency pair and is consistent in both conversion directions.
- Manual rate edits affect only the selected currency pair. `Keep this rate` protects that pair from API updates. Unpinned manual rates remain until the next successful API refresh.
- Settings are transactional: Save commits rate edits, pin changes and currency additions/removals; Cancel discards changes made in the settings screen.
- Result currencies can be reordered by dragging; the order persists across app restarts. Settings show the base currency first, followed by the remaining currencies in main-screen order.
- Conversion results use two decimal places; exchange rates on the main screen use four decimal places, with positive tiny rates displayed as at least 0.0001.

## Build from Android phone

Use Termux and run `bash scripts/build-install.sh` from the project directory. The script stages and commits source changes, pushes to `main`, waits for the matching GitHub Actions workflow, verifies the signed APK and downloads it to `Download/CurrencyConverter`. Source ZIP archives, APKs, build outputs and local signing keys are excluded from Git.

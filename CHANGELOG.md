# Changelog

## 1.0.6
- Replaced emoji/superscript rate-source markers with compact green circular badges containing `1` (Frankfurter) or `2` (ExchangeRate-API).
- Manual/fallback rates retain a blue badge without a number.
- Added unit tests for source badge labels.


## 1.0.5 (versionCode 6)
- Removed the keypad title and reduced the keypad height.
- Moved clear-all into the keypad as a regular key.
- Removed the empty-result ellipsis placeholder.
- Displayed amounts and rates with exactly two decimal places.
- Added long-press drag-and-drop reordering of result currencies.
- Persisted currency order in local preferences.


## 1.0.4 (versionCode 5)
- Fixed Android resource linking: added the missing round launcher icon referenced by AndroidManifest.xml.
- Kept the launcher icon artwork consistent between regular and round icon resources.

## 1.0.3 (versionCode 4)
- Renamed the app to Currency Converter.
- Translated the complete user interface and currency names into English.
- Added blue and red styling to the two C initials in the app title.
- Added a matching custom launcher icon.
- Kept the persistent release-signing configuration.

## 1.0.2 (versionCode 3)
- Added persistent release signing through GitHub Actions Secrets.
- Release APK is verified with apksigner before artifact publication.
- CI checks package ID, versionCode and versionName.

## 1.0.1 (versionCode 2)
- Added in-app version tracking.

## 1.0.13
- Fixed responsive layout for very large amounts and conversion results. Currency codes and rate labels stay on one line; result values shrink to fit their cards instead of forcing neighboring content to wrap or overflow.

## 1.0.12
- Added space-separated thousands grouping throughout displayed amounts and rates.
- Amount keypad and rate editor accept grouped values without breaking numeric parsing.

# Changelog

## 1.0.10 (versionCode 11)
- Made Settings Save/Cancel transactional and reliable.
- Save explicitly reads every rate field before committing the settings draft.
- Cancel restores the complete pre-edit snapshot, including rates, currencies and update interval.
- Prevented delayed EditText focus-loss callbacks from writing values after Save or Cancel closes the dialog.

## 1.0.9
- Reworked currency settings into transactional Cancel / Save behavior.
- Improved rate editor layout: compact flag and code column, wider rate input, circular remove control.
- Reorganized source legend into readable rows with numbered/blue badges.

## 1.0.8 (build 9)
- Removed the clear-all key from the numeric keypad. The bottom row now contains only decimal point, zero and backspace, aligned as three equal keys.


## 1.0.7 (build 8)
- Replaced the text-only rate source legend with the same circular source badges used in the currency list: green 1, green 2, and blue manual/fallback.

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

## 1.0.11
- Made result cards easier to reorder using a dedicated drag handle.
- Improved drag-and-drop target feedback and local drag-state handling.
- Currency order continues to persist in the saved repository order.
- Settings remain transactional: Save commits the current draft; Cancel restores the snapshot from when settings were opened.

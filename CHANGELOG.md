# Changelog

## 1.0.24
- Removed periodic rate-update intervals. Rates now refresh automatically once when the app is opened, and can also be refreshed manually with Update rates now.
- Update rates now fetches the complete provider rate set and updates every unpinned currency pair; pinned pair overrides remain unchanged.
- Fixed API source badges so USD/EGP shows the same provider in both directions. The source belongs to the underlying currency pair, not whichever currency happens to be the target.
- Pinning a rate now snapshots the currently displayed quote and its source as a pair-specific override, so the quote truly remains fixed across refreshes and base-currency changes.
- Added unit tests for source consistency in both directions and manual pair overrides.

## 1.0.21
- Settings now list the current base currency first, followed by the other currencies in exactly the same order as the main screen.
- Expanded the one-command release script's live progress display to show workflow jobs and individual build/test/signing steps, updating only when their state changes.

## 1.0.19
- Corrected the release archive and version progression; this release supersedes the incorrectly packaged 1.0.18 source archive.
- Kept the launcher icon as two diagonal C initials: blue upper-left and red lower-right.
- Kept the rate-source preservation fix so unchanged rounded rate fields do not overwrite API precision or source metadata.
- Hardened the single-command Termux release script: retries transient GitHub API failures, parses workflow status reliably, tracks the existing run for the pushed commit, and excludes the downloaded source ZIP from the Git commit.

## 1.0.18
- Restored the launcher icon concept with two diagonal C initials: blue upper-left and red lower-right.
- Prevented unchanged rounded rate fields from overwriting original API precision and source metadata when settings are saved.

## 1.0.17
- Fixed Update rates now inside the open settings dialog.
- Shows an Updating rates state while requests are running and reports success or failure.
- Refreshes the settings view after the API response so updated values and source badges are immediately visible.
- Keeps the current settings draft and respects pinned rates.


## 1.0.16
- Simplified release versioning: VERSION.txt is now the single source of truth.
- Android versionCode is derived automatically from the app version.
- Removed the separate build number from the in-app version display and About dialog.
- GitHub Actions artifact names and downloaded APK filenames are generated from the app version.

## 1.0.15
- The Termux build/install script renames the downloaded release APK using the app name and version from VERSION.txt.
- Aligned GitHub Actions artifact name and package metadata checks with version 1.0.15 / versionCode 16.


## 1.0.14
- Fixed rate-source badges so editing one currency no longer turns every currency blue.
- Manual edits affect only the edited currency pair.
- Added a per-currency Keep this rate checkbox. Pinned rates are preserved during automatic and manual API refreshes.
- Unpinned manual rates remain blue until the next successful API refresh replaces them.
- Unchanged rate fields no longer get marked as manual when Save is pressed.


## 1.0.12
- Added space-separated thousands grouping throughout displayed amounts and rates.
- Amount keypad and rate editor accept grouped values without breaking numeric parsing.

# Changelog

## 1.0.21
- Settings now list the current base currency first, followed by the other currencies in exactly the same order as the main screen.
- Expanded the one-command release script's live progress display to show workflow jobs and individual build/test/signing steps, updating only when their state changes.

## 1.0.10
- Made Settings Save/Cancel transactional and reliable.
- Save explicitly reads every rate field before committing the settings draft.
- Cancel restores the complete pre-edit snapshot, including rates, currencies and update interval.
- Prevented delayed EditText focus-loss callbacks from writing values after Save or Cancel closes the dialog.

## 1.0.9
- Reworked currency settings into transactional Cancel / Save behavior.
- Improved rate editor layout: compact flag and code column, wider rate input, circular remove control.
- Reorganized source legend into readable rows with numbered/blue badges.

## 1.0.8
- Removed the clear-all key from the numeric keypad. The bottom row now contains only decimal point, zero and backspace, aligned as three equal keys.


## 1.0.7
- Replaced the text-only rate source legend with the same circular source badges used in the currency list: green 1, green 2, and blue manual/fallback.

## 1.0.6
- Replaced emoji/superscript rate-source markers with compact green circular badges containing `1` (Frankfurter) or `2` (ExchangeRate-API).
- Manual/fallback rates retain a blue badge without a number.
- Added unit tests for source badge labels.


## 1.0.5
- Removed the keypad title and reduced the keypad height.
- Moved clear-all into the keypad as a regular key.
- Removed the empty-result ellipsis placeholder.
- Displayed amounts and rates with exactly two decimal places.
- Added long-press drag-and-drop reordering of result currencies.
- Persisted currency order in local preferences.


## 1.0.4
- Fixed Android resource linking: added the missing round launcher icon referenced by AndroidManifest.xml.
- Kept the launcher icon artwork consistent between regular and round icon resources.

## 1.0.3
- Renamed the app to Currency Converter.
- Translated the complete user interface and currency names into English.
- Added blue and red styling to the two C initials in the app title.
- Added a matching custom launcher icon.
- Kept the persistent release-signing configuration.

## 1.0.2
- Added persistent release signing through GitHub Actions Secrets.
- Release APK is verified with apksigner before artifact publication.
- CI checks package ID, versionCode and versionName.

## 1.0.1
- Added in-app version tracking.

## 1.0.11
- Made result cards easier to reorder using a dedicated drag handle.
- Improved drag-and-drop target feedback and local drag-state handling.
- Currency order continues to persist in the saved repository order.
- Settings remain transactional: Save commits the current draft; Cancel restores the snapshot from when settings were opened.
\n## 1.0.20\n- Keep freshly downloaded API rates when the settings dialog is cancelled; Cancel now rolls back only pending manual/settings edits.\n- Refine the launcher mark with bolder currency-inspired C letterforms and double currency cuts.\n
## 1.0.22
- Store manual exchange rates as currency-pair overrides instead of changing a currency's global USD-normalized factor.
- Preserve a manual pair rate and its source badge when switching the base currency; the reverse direction uses the reciprocal rate.
- Keep changes isolated to the edited pair, leaving all other currency-pair rates unchanged.
- Track the Keep this rate setting per pair and clear an unpinned manual pair override when an API refresh supplies a fresh rate for either currency.
- Add unit tests for reciprocal pair conversion and 1:1 rates in both directions.


## 1.0.23
- Remove accidentally committed source ZIP archives and the stray nested `cc/` extraction directory before each release commit.
- Add `.gitignore` rules preventing source archives, APKs, build outputs and local signing files from entering Git.
- Shorten live GitHub Actions progress refresh interval from 8 to 5 seconds.

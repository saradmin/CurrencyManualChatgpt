#!/data/data/com.termux/files/usr/bin/bash
# Wait for the GitHub Actions run for the current commit, download its
# versioned release artifact, verify it, and open the containing folder.
# APK installation is intentionally left to the user's file manager because
# Huawei/Android package installer rejects APKs launched from Termux on this device.

set -u

REPO="saradmin/CurrencyManualChatgpt"
WORKFLOW="build-apk.yml"
BRANCH="main"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION_FILE="$PROJECT_ROOT/VERSION.txt"
DOWNLOAD_ROOT="$HOME/storage/shared/Download/CurrencyConverter"

fail() {
  printf '\nERROR: %s\n' "$1" >&2
  exit 1
}

command -v gh >/dev/null 2>&1 || fail "GitHub CLI (gh) is not installed."
command -v git >/dev/null 2>&1 || fail "Git is not installed."
command -v unzip >/dev/null 2>&1 || fail "unzip is not installed. Run: pkg install unzip"
[ -f "$VERSION_FILE" ] || fail "VERSION.txt was not found at $VERSION_FILE"

if ! gh auth status >/dev/null 2>&1; then
  fail "GitHub CLI is not authenticated. Run: gh auth login"
fi

VERSION="$(sed -n 's/^Version: *//p' "$VERSION_FILE" | head -n 1)"
ARTIFACT="$(sed -n 's/^Release artifact: *//p' "$VERSION_FILE" | head -n 1)"
[ -n "$VERSION" ] || fail "Could not read Version from VERSION.txt"
[ -n "$ARTIFACT" ] || fail "Could not read Release artifact from VERSION.txt"

cd "$PROJECT_ROOT" || fail "Cannot enter project directory"
COMMIT="$(git rev-parse HEAD 2>/dev/null)" || fail "This directory is not a Git repository"
CURRENT_BRANCH="$(git branch --show-current)"
[ "$CURRENT_BRANCH" = "$BRANCH" ] || fail "Current branch is '$CURRENT_BRANCH', expected '$BRANCH'."

printf 'Currency Converter %s\n' "$VERSION"
printf 'Commit: %s\n' "$COMMIT"
printf 'Artifact: %s\n\n' "$ARTIFACT"
printf 'Waiting for GitHub Actions to register the workflow run for this commit.\n'
printf 'Polling every 5 seconds. Press Ctrl+C to stop.\n'

RUN_ID=""
while [ -z "$RUN_ID" ]; do
  RUN_ID="$(gh run list \
    --repo "$REPO" \
    --workflow "$WORKFLOW" \
    --branch "$BRANCH" \
    --commit "$COMMIT" \
    --limit 10 \
    --json databaseId,headSha \
    --jq ".[] | select(.headSha == \"$COMMIT\") | .databaseId" 2>/dev/null | head -n 1)"

  if [ -z "$RUN_ID" ]; then
    printf '.'
    sleep 5
  fi
done

printf '\nFound workflow run: %s\n' "$RUN_ID"
printf 'Waiting for build, tests, APK signature and metadata verification...\n'
# gh run watch can return non-zero for a failed workflow. We inspect the
# conclusion below and print the failed log ourselves.
gh run watch "$RUN_ID" --repo "$REPO"
WATCH_EXIT=$?

CONCLUSION="$(gh run view "$RUN_ID" --repo "$REPO" --json conclusion --jq '.conclusion' 2>/dev/null)"
if [ "$CONCLUSION" != "success" ]; then
  printf '\nBuild did not succeed (conclusion: %s, watch exit: %s).\n' "${CONCLUSION:-unknown}" "$WATCH_EXIT" >&2
  gh run view "$RUN_ID" --repo "$REPO" --log-failed || true
  exit 1
fi

printf '\nBuild succeeded. Downloading artifact %s...\n' "$ARTIFACT"
rm -rf "$DOWNLOAD_ROOT"
mkdir -p "$DOWNLOAD_ROOT" || fail "Cannot create download directory"

gh run download "$RUN_ID" \
  --repo "$REPO" \
  --name "$ARTIFACT" \
  --dir "$DOWNLOAD_ROOT" || fail "Artifact download failed"

APK="$DOWNLOAD_ROOT/app-release.apk"
[ -s "$APK" ] || fail "Downloaded artifact does not contain a non-empty app-release.apk"
unzip -t "$APK" >/dev/null 2>&1 || fail "Downloaded APK failed ZIP integrity check"

printf '\nAPK downloaded and archive integrity verified.\n'
printf 'Version: %s\n' "$VERSION"
printf 'File: %s\n' "$APK"
ls -lh "$APK"
if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$APK"
fi

printf '\nOpening the download folder (if Android allows it)...\n'
if command -v termux-open >/dev/null 2>&1; then
  termux-open "$DOWNLOAD_ROOT" >/dev/null 2>&1 || true
fi
printf '\nAutomatic APK installation is intentionally disabled.\n'
printf 'Open this file in ES File Explorer and install it:\n%s\n' "$APK"

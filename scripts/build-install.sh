#!/data/data/com.termux/files/usr/bin/bash
# One-command release flow: stage, commit, push, wait for the matching
# GitHub Actions run despite transient API/network failures, download and verify APK.
set -Eeuo pipefail

REPO="saradmin/CurrencyManualChatgpt"
WORKFLOW="build-apk.yml"
BRANCH="main"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION_FILE="$PROJECT_ROOT/VERSION.txt"
DOWNLOAD_ROOT="$HOME/storage/shared/Download/CurrencyConverter"
POLL_SECONDS=5
MAX_API_RETRIES=20

fail() { printf '\nERROR: %s\n' "$1" >&2; exit 1; }

for tool in git gh unzip; do command -v "$tool" >/dev/null 2>&1 || fail "$tool is not installed."; done
[ -f "$VERSION_FILE" ] || fail "VERSION.txt was not found."
gh auth status >/dev/null 2>&1 || fail "GitHub CLI is not authenticated. Run: gh auth login"

cd "$PROJECT_ROOT"
CURRENT_BRANCH="$(git branch --show-current)"
[ "$CURRENT_BRANCH" = "$BRANCH" ] || fail "Current branch is '$CURRENT_BRANCH'; expected '$BRANCH'."

APP_NAME="$(sed -n 's/^App name: *//p' "$VERSION_FILE" | head -n 1)"
VERSION="$(sed -n 's/^Version: *//p' "$VERSION_FILE" | head -n 1)"
[ -n "$APP_NAME" ] && [ -n "$VERSION" ] || fail "Could not read app name/version from VERSION.txt."
grep -Fq "**Version:** ${VERSION}" "$PROJECT_ROOT/README.md" || fail "README version does not match VERSION.txt."
grep -Fq "CurrencyConverter-v${VERSION}-source-for-git.zip" "$PROJECT_ROOT/README.md" || fail "README source archive name does not match VERSION.txt."
ARTIFACT="CurrencyConverter-v${VERSION}-release-apk"

printf 'Currency Converter %s\n' "$VERSION"
printf '\n[1/4] Cleaning repository and preparing Git commit...\n'
# Remove source ZIPs copied into the project folder for extraction. They are
# delivery artifacts, not source code, and must never enter Git history.
find "$PROJECT_ROOT" -maxdepth 1 -type f \
  \( -name 'CurrencyConverter-*.zip' -o -name 'CurrencyManual-*.zip' \) -delete
# Remove the accidental nested extraction directory seen in the repository.
[ ! -d "$PROJECT_ROOT/cc" ] || rm -rf "$PROJECT_ROOT/cc"
# Stage all source changes, including deletions of previously committed ZIPs.
git add -A
if ! git diff --cached --quiet; then
  git commit -m "Currency Converter ${VERSION}"
else
  printf 'No source changes to commit; using current HEAD.\n'
fi
COMMIT="$(git rev-parse HEAD)"
printf '\n[2/4] Pushing %s to origin...\n' "$COMMIT"
PUSH_DELAY=5
PUSH_ATTEMPTS=0
until git push origin "$BRANCH"; do
  PUSH_ATTEMPTS=$((PUSH_ATTEMPTS + 1))
  [ "$PUSH_ATTEMPTS" -lt 8 ] || fail "Git push failed after $PUSH_ATTEMPTS attempts. Check network/authentication and rerun this script."
  printf 'Push failed; retrying in %ss...\n' "$PUSH_DELAY" >&2
  sleep "$PUSH_DELAY"
  [ "$PUSH_DELAY" -ge 30 ] || PUSH_DELAY=$((PUSH_DELAY + 5))
  [ "$PUSH_DELAY" -le 30 ] || PUSH_DELAY=30
done

# Retry transient GitHub API/network errors. The retry limit prevents an
# authentication/configuration error from leaving the script running forever.
retry_gh() {
  local attempt=0 delay=5 output
  while true; do
    if output="$(gh "$@" 2>&1)"; then
      printf '%s' "$output"
      return 0
    fi
    attempt=$((attempt + 1))
    printf '\nGitHub API request failed (%s/%s): %s\n' "$attempt" "$MAX_API_RETRIES" "$output" >&2
    [ "$attempt" -lt "$MAX_API_RETRIES" ] || return 1
    printf 'Retrying in %ss...\n' "$delay" >&2
    sleep "$delay"
    [ "$delay" -ge 30 ] || delay=$((delay + 5))
    [ "$delay" -le 30 ] || delay=30
  done
}

printf '\n[3/4] Waiting for GitHub Actions run for this commit...\n'
RUN_ID=""
while [ -z "$RUN_ID" ]; do
  RUN_ID="$(retry_gh run list --repo "$REPO" --workflow "$WORKFLOW" --branch "$BRANCH" --commit "$COMMIT" --limit 10 --json databaseId,headSha --jq '.[0].databaseId // empty')" || fail "Could not query GitHub Actions after repeated retries. The commit is pushed; rerun this script to resume."
  [ -n "$RUN_ID" ] || { printf '.'; sleep "$POLL_SECONDS"; }
done
printf '\nFound workflow run: %s\n' "$RUN_ID"

printf '\n[4/4] Live GitHub Actions progress (run %s)\n' "$RUN_ID"
LAST_PROGRESS=""
while true; do
  STATE="$(retry_gh run view "$RUN_ID" --repo "$REPO" --json status,conclusion --jq '[.status, (.conclusion // "")] | @tsv')" || fail "Could not read workflow status after repeated retries. Run $RUN_ID is still on GitHub; rerun this script to resume."
  IFS=$'\t' read -r STATUS CONCLUSION <<< "$STATE"
  PROGRESS="$(retry_gh run view "$RUN_ID" --repo "$REPO" --json jobs --jq '.jobs[]? as $job | $job.steps[]? | (($job.name) + " / " + .name + ": " + (if .status == "completed" then (if .conclusion == "success" then "✓ success" elif .conclusion == "skipped" then "- skipped" else "✗ " + (.conclusion // "failed") end) else (.status // "queued") end))')" || fail "Could not read workflow job progress after repeated retries. Run $RUN_ID is still on GitHub; rerun this script to resume."
  if [ "$PROGRESS" != "$LAST_PROGRESS" ] && [ -n "$PROGRESS" ]; then
    printf '\n%s\n' "$PROGRESS"
    LAST_PROGRESS="$PROGRESS"
  fi
  if [ "$STATUS" = "completed" ]; then break; fi
  printf '\nOverall workflow: %s. Refreshing in %ss...\n' "${STATUS:-unknown}" "$POLL_SECONDS"
  sleep "$POLL_SECONDS"
done

if [ "$CONCLUSION" != "success" ]; then
  printf '\nGitHub Actions finished: %s\n' "${CONCLUSION:-unknown}" >&2
  gh run view "$RUN_ID" --repo "$REPO" --log-failed || true
  fail "Build failed. No APK was downloaded."
fi

printf '\nBuild succeeded. Downloading artifact...\n'
mkdir -p "$DOWNLOAD_ROOT"
rm -f "$DOWNLOAD_ROOT/app-release.apk"
retry_gh run download "$RUN_ID" --repo "$REPO" --name "$ARTIFACT" --dir "$DOWNLOAD_ROOT" >/dev/null || fail "Artifact download failed after repeated retries."
APK_SOURCE="$DOWNLOAD_ROOT/app-release.apk"
[ -s "$APK_SOURCE" ] || fail "Artifact does not contain app-release.apk."
unzip -t "$APK_SOURCE" >/dev/null 2>&1 || fail "APK integrity check failed."
SAFE_NAME="$(printf '%s' "$APP_NAME" | tr '[:upper:] ' '[:lower:]-' | tr -cd 'a-z0-9._-')"
APK="$DOWNLOAD_ROOT/${SAFE_NAME}-v${VERSION}.apk"
mv -f "$APK_SOURCE" "$APK"
printf '\nSUCCESS: APK downloaded and verified.\n%s\n' "$APK"
ls -lh "$APK"
command -v sha256sum >/dev/null 2>&1 && sha256sum "$APK" || true
printf '\nOpen the APK in ES File Explorer to install it.\n'
command -v termux-open >/dev/null 2>&1 && termux-open "$APK" >/dev/null 2>&1 || true

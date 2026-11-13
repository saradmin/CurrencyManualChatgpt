# Build Currency Converter from Termux

From the project root on the phone:

```bash
git add -A
git commit -m "Currency Converter 1.0.16"
git push origin main
bash scripts/build-install.sh
```

The script waits for the GitHub Actions run matching the pushed commit, checks tests and signed release build, downloads the matching artifact, validates the APK and renames it using the app name and version from `VERSION.txt`. Installation remains manual through ES File Explorer.

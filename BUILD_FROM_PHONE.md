# Build Currency Converter from Termux

From the project root on the phone:

```bash
unzip -o ~/storage/shared/Download/CurrencyConverter-v1.0.11-source-for-git.zip -d .
git add -A
git commit -m "Currency Converter 1.0.11: settings and reorder"
git push origin main
bash scripts/build-install.sh
```

The script waits for the GitHub Actions run matching the pushed commit, checks that tests and the signed release build succeeded, downloads the versioned APK artifact, and verifies the APK archive. Installation remains manual through ES File Explorer because Android's package installer rejected launching the APK from Termux on this Huawei device.

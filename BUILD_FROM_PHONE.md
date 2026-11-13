# Build and install from an Android phone

From the project root, copy the versioned source ZIP from Downloads, unpack it, commit and push. Then run the reusable build script:

```bash
cd ~/storage/shared/projects/CurrencyManualChatgpt
cp ~/storage/shared/Download/CurrencyConverter-v1.0.6-source-with-build-script.zip .
unzip -o CurrencyConverter-v1.0.6-source-with-build-script.zip -d .
git add -A
git commit -m "Update build-install workflow"
git push origin main
bash scripts/build-install.sh
```

The script waits for the GitHub Actions run matching the current commit, checks the workflow result, downloads the versioned release APK, and validates the APK archive. It does not launch Android's package installer because launching the APK from Termux has failed on the target Huawei device. It may open the download folder; install `app-release.apk` manually using ES File Explorer.

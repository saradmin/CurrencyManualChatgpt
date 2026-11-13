# Build Currency Converter from phone

## One-time release signing setup

1. Generate a private Android signing keystore in Termux. Keep the `.jks` file private and back it up securely.
2. Add these GitHub Actions repository secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
3. Never commit or upload the `.jks` file to the repository. Never share the passwords in chat.
4. The workflow builds a signed release APK, verifies its signature with `apksigner`, and checks package/version metadata before publishing the artifact.

Example keystore generation (run once; choose and save your own strong passwords):

```bash
mkdir -p ~/private/currencymanual-signing
chmod 700 ~/private/currencymanual-signing
keytool -genkeypair -v \
  -keystore ~/private/currencymanual-signing/currencymanual-release.jks \
  -alias currencymanual \
  -keyalg RSA -keysize 4096 -validity 10000
```

Create a base64 file for uploading as the `ANDROID_KEYSTORE_BASE64` secret:

```bash
base64 -w 0 ~/private/currencymanual-signing/currencymanual-release.jks \
  > ~/private/currencymanual-signing/keystore.base64
```

Use GitHub CLI to set secrets without putting their values in shell history:

```bash
gh secret set ANDROID_KEYSTORE_BASE64 < ~/private/currencymanual-signing/keystore.base64
read -s -p 'Keystore password: ' STORE_PASS; echo
printf '%s' "$STORE_PASS" | gh secret set ANDROID_KEYSTORE_PASSWORD
unset STORE_PASS
read -s -p 'Key password: ' KEY_PASS; echo
printf '%s' "$KEY_PASS" | gh secret set ANDROID_KEY_PASSWORD
unset KEY_PASS
echo 'currencymanual' | gh secret set ANDROID_KEY_ALIAS
```

The workflow requires all four secrets before building a release APK.

## Download and install

After a successful workflow run, download artifact `CurrencyConverter-v1.0.3-release-apk`. Install `app-release.apk`.

The currently installed app was signed with a different key. To migrate to this permanent key, uninstall the old app once, then install this signed release. Future releases must use the same keystore and passwords or Android will reject in-place updates.

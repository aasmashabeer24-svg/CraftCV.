# CraftCV: secure AAB signing

Never commit a .jks/.keystore file, store passwords or a base64 signing key. Choose and safely back up an upload key before publishing.

1. Create your upload key locally with a Java 17 JDK:

```bash
keytool -genkeypair -v -keystore craftcv-upload.jks -keyalg RSA -keysize 3072 -validity 10000 -alias craftcv-upload
```

2. Save the keystore and passwords securely outside GitHub.
3. In GitHub > Settings > Secrets and variables > Actions, configure:
   - `CRAFTCV_KEYSTORE_BASE64`: single-line base64 encoding of the .jks bytes.
   - `CRAFTCV_STORE_PASSWORD`
   - `CRAFTCV_KEY_ALIAS`
   - `CRAFTCV_KEY_PASSWORD`
4. In Actions > Android CI & release bundle > Run workflow, run the manual release job.
5. Download `craftcv-signed-release-aab` from the completed run.
6. Verify signature with `jarsigner -verify -verbose -certs app-release.aab`.
7. Upload the bundle in Play Console and enroll in Play App Signing.

The CI workflow deliberately does not provide a signed AAB if any signing secret is missing.

# CraftCV — Android app & independent-hosting preparation

This repository contains the **Android app shell** (Java WebView, AdMob test mode, Gradle and Play-release workflow) for CraftCV. It is **not** the CraftCV website source code or backend.

The current fallback website is https://craftcv-hees13.v2.appdeploy.ai/. An Android app built from this repository continues to depend on that website, its authentication, storage and AI services until those are independently migrated.

## Android builds

Requires Java 17, Android SDK 36 and Gradle 8.13 (or run the included GitHub Actions workflow).

```bash
gradle :app:lintDebug :app:assembleDebug
```

To test a **verified, working** independent HTTPS website without hardcoding a new domain in Java:

```bash
gradle :app:lintDebug :app:assembleDebug -PcraftcvWebUrl=https://cv.example.com/
```

Or set the `CRAFTCV_WEB_URL` environment variable. It takes precedence over the Gradle property. Both require an HTTPS **origin** only (no path, port, query or fragment). The Android WebView trusts only that exact host/port and opens external sites in the browser.

The GitHub Actions **Android CI & release bundle** manual workflow also accepts a `web_url` input. A blank input retains the existing site. This builds an alternate Android package; it **does not deploy or copy the website**, move saved resumes, or transfer user sessions.

## Before public release

- Read [Independent hosting & migration](docs/INDEPENDENT_HOSTING_MIGRATION.md).
- Read [Play Console checklist](docs/PLAY_CONSOLE_CHECKLIST.md) and [Data Safety worksheet](docs/DATA_SAFETY_WORKSHEET.md).
- Sign the release AAB securely using [release signing instructions](docs/RELEASE_SIGNING.md).
- Test login, account deletion, resume import, PDF/DOCX export and device behavior against the *final* site.
- Keep the app unpublished until policies, permissions, website rights and real-device acceptance checks are complete.

Package ID: `com.craftcv.resumestudio`. Current release build deliberately contains **no production ad unit** (only a test banner in debug mode).

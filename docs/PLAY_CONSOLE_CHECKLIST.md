# CraftCV — Google Play Console release checklist

The Android shell in this repository uses the live CraftCV web app. Do not claim the app is store-ready without verifying all items below.

## Technical
- [x] Android Gradle project created, with `targetSdk 36`, `compileSdk 36`.
- [x] Network-only manifest permissions: INTERNET + ACCESS_NETWORK_STATE.
- [x] HTTPS-only WebView for the CraftCV host; external links hand off to an Android browser.
- [x] File chooser support for photo/document input.
- [x] Android CI workflow for lint + debug APK; manual release AAB job *requires* signing secrets.
- [x] GitHub Actions debug build + lint passed on 2026-11-14 (run #10). Repeat the checks against the independent hosting branch and final production domain.
- [ ] Confirm Google login and AppDeploy redirect authentications work inside WebView. OAuth providers may reject embedded browsers; integrate supported native or Custom Tabs login if needed.
- [ ] Confirm PDF printing, Word downloads, photo picker, cloud sync, and account deletion on Android.
- [ ] Android release bundle must be signed and verified.
- [ ] Decide final permanent application ID before first Play release. Current provisional ID: `com.craftcv.resumestudio`.
- [ ] Enable Play App Signing; securely store upload key outside GitHub and set repository Actions secrets.
- [ ] Install on at least two actual Android versions/devices and check back navigation, edge-to-edge, keyboard, color contrast, crashes, accessibility and offline state.

## Policies / privacy
- [ ] Publish a **real** publicly accessible HTTPS privacy policy on the app's own public website (not only this repo); verify link from app and Play listing.
- [ ] Publish Terms of Service / support contact with verified publisher identity.
- [ ] Implement **completed account deletion**, not only logging a request: delete account, cloud resumes, user-related data and provider records or clearly disclose lawful retention.
- [ ] Publish accessible **web account-deletion request page** that does not require reinstalling the Android app.
- [ ] Test deletion flows end-to-end with separate accounts, including deletion of user data from service providers where applicable.
- [ ] Review AI, authentication, analytics, hosting and storage practices; complete Data Safety answers **based on actual behavior**.
- [ ] Provide a privacy policy URL, data deletion URL, data-handling disclosures, security contacts and retention periods.
- [ ] Use accurate app access instructions/test login for Play reviewers if required.
- [ ] Confirm children/age targeting, advertising, developer-provided data and other declarations from actual behavior.

## Store listing
- [ ] Final app name, short description (80 characters max), full description, category, support email and website.
- [ ] 512 x 512 PNG Play icon and 1024 x 500 PNG feature graphic. SVG design sources may be exported to PNG.
- [ ] Real Android device screenshots (at least 2 phone shots) showing the app working (not mock screenshots).
- [ ] Set countries/regions, distribution, monetization, ads declaration and content rating questionnaire.
- [ ] If account type requires it, 12 testers opted in continuously for 14 days in closed testing, then request production access.
- [ ] Upload signed AAB via Google Play Console and finish all warnings and pre-launch reports.

## Important limitations

The Android website origin is now build-time configurable (`CRAFTCV_WEB_URL` / `-PcraftcvWebUrl`). See `docs/INDEPENDENT_HOSTING_MIGRATION.md`. This does **not** migrate the website or user data.

This repository contains a **WebView wrapper** for the existing live web app, not the website source. If AppDeploy is down, many core features will not be available. Google may reject low-functionality WebView wrappers. Resolve login/print limitations before production. Never claim the bundle is approved simply because it compiles.

As of 2026-11-14 Google's 2026 requirement for new phone apps is Android 16 / API level 36.
Official sources: https://support.google.com/googleplay/android-developer/answer/11926878
https://support.google.com/googleplay/android-developer/answer/10144311
https://support.google.com/googleplay/android-developer/answer/14151465

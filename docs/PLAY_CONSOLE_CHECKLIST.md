# CraftCV — Google Play release readiness

Last reviewed: 19 November 2026  
Android branch: `feature/native-offline-craftcv`  
Online Studio: https://craftcv-hees13.v2.appdeploy.ai/  
Proposed Android package ID: `com.craftcv.resumestudio`

**Status:** Not ready for production submission yet. The Android project has a native offline CV builder and an optional Online Studio WebView; it is not merely a WebView wrapper. Some Google Play requirements require owner information, account-policy decisions and actions inside the Play Console.

## 1. Verified technical configuration

- [x] Native Android app project and four-step offline CV builder.
- [x] `compileSdk 36`, `targetSdk 36`, `minSdk 26`.
- [x] Current Play target requirement for new phone apps is Android 16 / API 36 (from 31 August 2026).
- [x] Package ID currently configured as `com.craftcv.resumestudio`; lock it before creating the Play app record.
- [x] Current version is `versionCode 1`, `versionName 1.0.0`.
- [x] Manifest currently declares INTERNET and ACCESS_NETWORK_STATE; no storage, contacts, SMS, call-log or location permissions.
- [x] Native offline CV records and photos are stored in app-private local storage; they are not automatically synced to cloud.
- [x] PDF and DOCX exports use Android's document picker; local CV creation/editing does not require sign-in.
- [x] CI runs unit tests, Android lint and a debug APK build.
- [ ] Latest mobile keyboard/viewport fix must pass the current Android CI run and physical phone acceptance.
- [ ] Install and test on at least two Android devices / Android versions; verify navigation, keyboard, accessibility, photo import, multi-page PDF, DOCX, file sharing and offline use.
- [ ] Generate a signed release AAB. The pull-request CI job creates a **debug APK only**.

## 2. Release signing

The workflow creates a signed release AAB only when manually dispatched and all four GitHub Actions secrets are present:

- `CRAFTCV_KEYSTORE_BASE64`
- `CRAFTCV_STORE_PASSWORD`
- `CRAFTCV_KEY_ALIAS`
- `CRAFTCV_KEY_PASSWORD`

Create and securely back up the upload keystore locally. Add the values through GitHub repository Settings → Secrets and variables → Actions. Never paste keystore passwords or private key material into ChatGPT or commit them to the repository. Enable Play App Signing in Play Console and keep a separate secure backup of the upload key.

AdMob secrets are optional. Without valid owner IDs, the release build disables live ads:
- `CRAFTCV_ADMOB_APP_ID`
- `CRAFTCV_ADMOB_BANNER_UNIT_ID`

Debug builds use Google's demo IDs only. If production ads are enabled, review the final binary and complete the Play Ads and SDK data disclosures accurately.

## 3. Policy and privacy blockers

### Privacy policy — draft created, not yet published

Every Play app needs an active, public HTTPS privacy-policy webpage and a privacy link within the app. The policy must describe this app's real data flows and service providers; it cannot be a generic template or a PDF-only link.

A source draft is available at `docs/PRIVACY_POLICY_DRAFT.md`. It accurately distinguishes local offline CVs, optional cloud CV uploads, selected AI text processing and Google Mobile Ads / UMP. The draft is intentionally marked NOT FOR PUBLICATION: it still needs the public publisher identity and a monitored support email, plus a finalized retention/deletion policy. The current repository does not contain a published privacy-policy URL or an in-app privacy page.

### Account deletion — not yet implemented

The optional Online Studio Cloud Account flow allows users to sign in / create an account. Deleting an individual cloud CV is not the same as deleting the account. Google requires a readily discoverable in-app deletion path and an external web deletion-request URL, and associated account data must actually be deleted.

The AppDeploy Auth SDK reference available to this project exposes sign-in, getUser and sign-out, but no account-delete method. The user can delete individual cloud CVs, but that does not delete the authentication account. **This remains a hard Play release blocker.** Do not claim account deletion is complete until a supported provider deletion path is implemented and tested. If the account feature remains in the Android release, resolve this before submission. Another release strategy is to remove account creation/cloud access from the Play build until deletion is supported.

### AI endpoint — bounded, but still public

The Online Studio backend applies a shared compare-and-swap quota of 100 AI requests per UTC day. The endpoint remains unauthenticated and the quota is global to the app rather than per user; this is a cost guardrail, not identity-based abuse prevention. Online AI now supports summary rewriting, work-experience bullets, project descriptions and job-description comparison with input validation and clearer provider error responses. The native AI screen currently supports summary, experience bullets and job-description review. Both AI paths send selected CV text only, not name, email, phone or photo. The AI service is an online third-party processor. Verify live responses, 429 behavior and disclose this data flow in the final privacy policy before production.

### Data Safety — form not completed

Data Safety must be based on the final release binary and all SDKs / WebView behavior, not just the native Java source. Current source review indicates:

- Offline native resumes, education, skills and profile photos are stored locally in app-private storage; the native editor does not automatically upload them.
- Optional Cloud Account uses AppDeploy authentication and stores the resume JSON on the online backend when a user explicitly signs in and uploads/saves a cloud resume. Resume JSON can contain contact details, education, skills, work history, projects and a profile photo.
- Optional AI sends only the selected role, summary, skills, experience notes and optional job description to the online AI service when the user taps Generate.
- Google Mobile Ads and UMP SDKs are included. Debug uses Google demo ad IDs; production ad requests are disabled unless valid owner IDs are configured. Review the SDK's current disclosures for the exact release build.
- HTTPS is used for online endpoints. Do not mark all data as “not collected” if cloud or AI features remain enabled.

Complete Data Safety after deciding which optional online features and ads are in the production app.

## 4. App Content declarations to complete in Play Console

- [ ] Privacy policy URL.
- [ ] Ads declaration: answer from the actual release build (Yes if production ads are enabled/shown).
- [ ] App access instructions. Offline CV creation needs no account; if online Cloud/AI requires sign-in, provide reviewer access and valid test credentials or explain optional access.
- [ ] Target audience and content (choose the actual age groups; do not target children unless the product is designed for them).
- [ ] IARC content rating questionnaire.
- [ ] Data Safety form and deletion-request mechanism answers.
- [ ] Any other declarations shown in the Console for the final app and monetization model.

## 5. Store listing assets and metadata

- [ ] Final app title (30-character limit), short description (80-character limit), full description, category, tags and contact details.
- [x] 512 × 512 PNG app icon generated (32-bit RGBA, 258 KB; download from the current ChatGPT file attachment before uploading to Play Console).
- [x] 1024 × 500 PNG feature graphic generated (24-bit RGB, 74 KB; download from the current ChatGPT file attachment before uploading to Play Console).
- [ ] Upload both PNG assets to the Play Console store listing after creating the app entry.
- [ ] At least two real phone screenshots of the fixed app. Do not use the bug screenshots supplied before the fixes.
- [ ] Store listing support email and website.
- [ ] Countries / regions and free-or-paid choice.
- [ ] Public privacy-policy URL and, if account creation remains, public account-deletion URL.

## 6. Testing and rollout

- [ ] Create the CraftCV app entry in Play Console using the final package ID.
- [ ] Upload the signed AAB to Internal Testing and test install/update.
- [ ] Run the Play pre-launch report and fix crashes, compatibility, accessibility and policy issues.
- [ ] Run Closed Testing and collect real feedback.
- [ ] For personal developer accounts created after 13 November 2023, Google requires at least 12 testers opted in continuously for 14 days before applying for production access. This does not apply to every developer account; confirm the account type and creation date.
- [ ] Apply for production access if required, then submit the production release.

## 7. Current source-level release blockers

1. Privacy policy draft exists but is not published; the public support email and publisher identity are not confirmed.
2. Cloud account creation is available, but full account deletion is not supported by the current Auth SDK integration. This is a release blocker unless Cloud Account creation is removed from the Play build.
3. AI endpoint has a shared daily quota but remains unauthenticated and does not provide per-user abuse protection.
4. Signed AAB has not been built; release signing secrets are not yet confirmed.
5. Play Console declarations, real device screenshots and final store assets still need submission/verification.
6. Latest native UI and structured work-experience changes still require CI completion and real-device verification.

## Official Google Play references

- Target API: https://support.google.com/googleplay/android-developer/answer/11926878
- App content / privacy policy / ads / reviewer access: https://support.google.com/googleplay/android-developer/answer/9859455
- Data Safety and SDK disclosures: https://support.google.com/googleplay/android-developer/answer/10787469
- Account deletion: https://support.google.com/googleplay/android-developer/answer/13327111
- New personal account testing: https://support.google.com/googleplay/android-developer/answer/14151465
- Store listing graphics: https://support.google.com/googleplay/android-developer/answer/9866151

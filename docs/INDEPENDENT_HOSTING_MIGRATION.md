# CraftCV — independent-hosting & standalone Android migration plan

**Status: preparation only.** This repository contains the Android WebView wrapper but **not** the website's React/frontend or backend source. Switching `CRAFTCV_WEB_URL` is not a website migration. Do not publish an empty copy, inject CSS to hide platform badges, or assume cloud data moves automatically.

## What must be recovered/implemented

1. Export or obtain the full CraftCV website source including build scripts, assets, fonts licensed for redistribution, resume templates, visual editor, export workers, accessibility and all working routes. Confirm source ownership and licenses.
2. Inventory the actual backend: accounts/auth provider, cloud resume database, file storage, AI services, env vars, background jobs, email, subscriptions, and data deletion. Never copy API keys into the Android app or commit them to GitHub.
3. Select independent hosting with HTTPS and deploy both website and supported backend (or replace platform-specific APIs with equivalent owned services). A static hosting provider alone cannot replace an auth/database/AI backend.
4. Provision a custom domain and working TLS certificate. Verify the independent site directly in a browser *without* AppDeploy branding, and verify all login and logout redirects.
5. Plan user migration: confirm export rights and privacy obligations; back up, map user IDs and resume records; migrate only with appropriate authorization, encryption and rollback plan. Test restored data with separate accounts before any DNS or Android cut-over.
6. Publish accessible privacy policy, terms, support and web deletion request URL. In-app deletion must actually delete account and associated cloud data (subject to disclosed lawful retention).
7. Test resume gallery/editor, AI operations, photo upload, PDF/DOCX downloads, cloud sync, password recovery, account deletion and data retention from browser **and** Android WebView. The current wrapper does not prove that blob URLs or embedded OAuth work.
8. Build the Android app against the new verified origin (below), run lint, inspect APK/AAB permissions, test on real devices and verify release signing. Release only after the final website and Play Console disclosures pass.

## Configure the Android wrapper

Local example (substitute a domain you control and have verified):

```bash
gradle :app:lintDebug :app:assembleDebug -PcraftcvWebUrl=https://cv.example.com/
```

GitHub: **Actions → Android CI & release bundle → Run workflow → web_url**. Enter the complete HTTPS origin. This same origin will be embedded in the manual signed release if signing secrets exist. The default remains `https://craftcv-hees13.v2.appdeploy.ai/` for backwards compatibility until migration is complete.

The Android code derives the allowed in-app host from the build-time URL; links to other origins open in the external browser. A changed host may invalidate authentication cookies or redirect flows. The app will still need a live network connection. Do **not** configure the new origin while it is still a placeholder.

## Constraints and unresolved blockers

- Existing website source/backend and hosted account data are not present in this GitHub repository.
- The app is presently a WebView, not a native offline resume editor. AppDeploy outages still affect it until the full service is migrated.
- Embedded Google/OAuth logins can be unsupported, and WebView `blob:` downloads may need a dedicated native export bridge. Verify; do not treat them as tested.
- Native Android AdMob is restricted to debug-only Google test ads. A separate approved Standard production ad unit, consent/privacy options and updated disclosures are prerequisites for real ads.
- Before rollout, maintain the same Play package ID `com.craftcv.resumestudio` and protect the upload signing key. Changing the application ID would create a different Play listing.
- No production DNS, data migration, third-party accounts or Play submission is performed by the configuration change alone.

## Acceptance criteria

- [ ] Owned website/backend source and migration rights verified.
- [ ] Independent HTTPS production site deployed and tested; no platform overlays.
- [ ] Secure authentication, AI, storage, resume export and account deletion verified.
- [ ] Existing user data safely migrated (if appropriate) and rollback tested.
- [ ] Android debug/release builds point to the final domain and device smoke tests pass.
- [ ] Privacy links, Data Safety disclosure, signed AAB and Play Console listing completed.

# CraftCV — feature audit and Android device verification

Updated: 18 November 2026. Android branch: `feature/native-offline-craftcv`; online app: `craftcv-hees13`.

**Status rule:** A successful build, unit test, or web deployment QA is not proof that every interaction works on a physical Android phone or with a live user account.

## Current verification

| Capability | Implemented in source | Automated / deployment checks | Still pending |
|---|---|---|---|
| Native dashboard and separate screens | Yes | Android compile + lint passed | Phone layout, navigation and system Back |
| Four-step offline CV builder and autosave | Yes | Compile + lint passed | Enter, save, relaunch and reopen on phone |
| Saved CV library, duplicate and delete | Yes | Compile + lint passed | Persistence and original-preservation test |
| Native templates | 12 styles | Compile + lint passed | Compare actual rendered PDF styles on phone |
| Online template gallery | 14 styles and separate hash-routed pages | Latest AppDeploy deployment ready; no frontend/network errors | Real mobile interaction and browser Back |
| Profile photo | Android picker, local image and PDF inclusion | Compile + lint passed | JPG/PNG, orientation and PDF test on phone |
| Native PDF preview/export/share | A4 PDF generation, renderer, Android Save As and open/share | Compile + lint passed | Long multi-page PDF, external viewer and print test |
| Native DOCX export | Editable Office Open XML ZIP document | JUnit verifies ZIP parts, XML escaping and content | Open in Microsoft Word / Google Docs on Android and desktop |
| Cover letter | Offline editor and PDF export | Compile + lint passed | Save/reopen/export test on phone |
| Native AI Assistant | Optional HTTPS summary, bullets and job-description review; apply only after confirmation | Compile + lint passed; endpoint exists | Live request, failure handling, cost/rate protection and phone test |
| Cloud Account | Separate native entry opens authenticated Online Studio | Compile + lint passed; cloud APIs require auth | Live sign-in, create/edit/delete and cross-device test |
| Native-to-cloud sync | Not implemented | Not applicable | Offline native CVs remain local; manual online upload required |
| Online Studio | Optional WebView and browser; separate Build, Templates, Finished CV, AI and Cloud pages | Latest AppDeploy status ready; frontend/network errors empty | WebView login, Android file picker and blob downloads on phone |
| Debug AdMob banner | Google demo IDs, one labelled adaptive banner on Dashboard only, UMP consent gate | Compile + lint passed | Actual demo impression, consent and privacy-options test |
| Production ads | Disabled unless owner supplies valid AdMob App ID and banner unit ID at release build | Build configuration reviewed | Owner IDs, AdMob app review, privacy policy, Play declarations |
| Play Store package | Workflow supports signed release AAB on manual dispatch | Latest CI run generated debug APK only | Signing secrets, signed AAB, internal testing and Play review |

## Latest CI evidence

GitHub Actions run `44053058550` completed successfully. The job passed:
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- Debug APK artifact upload

The same run skipped release signing and signed AAB generation. Its artifact is `craftcv-debug-apk`, not a Play Store release.

## Ads and privacy

- Debug builds use Google's official demo App ID and banner unit. They are test ads and do not earn revenue.
- Production ads remain disabled until both valid owner IDs are supplied to the release build.
- The native banner is placed on the dashboard only; no ads are placed in the editor, preview, PDF, Word or cover-letter documents.
- UMP consent is requested before ad requests; privacy options are displayed when required.
- Before release, configure AdMob privacy messages, publish a privacy policy, and complete Play Console Contains Ads and Data safety declarations.
- The AI endpoint accepts only the requested role, summary, skills, experience notes and optional job description from the native screen; it does not send the name, email, phone or photo. **The current AI route is not authenticated or rate-limited in the backend source; add abuse/cost protection before public production use.**
- Cloud resume CRUD routes use authenticated requests and user-scoped storage. Native offline resumes do not automatically sync to cloud.

## Required physical Android acceptance test

1. Back up existing CVs. Install the debug update over the existing debug build if possible; do not uninstall first.
2. Create a CV and fill all four builder steps. Navigate Back/Continue, close the app, reopen and confirm autosave.
3. Open Templates and apply at least four different styles. Confirm the actual PDF design changes.
4. Import JPG and PNG portraits; verify orientation and PDF inclusion/removal.
5. Open Finished CV; confirm a nonblank A4 preview, including long multi-page content.
6. Save PDF and DOCX using Android's document picker. Open both in external compatible apps; share the PDF.
7. Duplicate a CV, edit/delete the copy, and confirm the original remains unchanged after restart.
8. Create and reopen a cover letter; export and open its PDF.
9. Turn internet off and repeat native editing, templates, PDF and DOCX. These offline workflows should remain available.
10. Turn internet on. Test AI summary, bullets and job matching; test a service failure and verify CV data is not changed unless the user applies a suggestion.
11. Sign in to Cloud Account, create/edit/delete an online CV and open it on another browser/device. Confirm local offline CVs are not represented as synced.
12. Verify the debug demo banner and UMP consent/privacy choices. Confirm no ad appears in editor, preview or exports.
13. Test on a small Android screen and system Back from every page.
14. Only after all device checks pass: configure signing and AdMob, build signed AAB, complete privacy/ads declarations and run Play Console internal testing.

## Known limitations / release blockers

- Native DOCX is editable text and does not reproduce every artistic PDF theme or include the profile photo.
- Native offline CVs do not automatically sync with the cloud account.
- AI and Cloud require internet; the AI route needs abuse/cost protection before public launch.
- Android device tests and live account tests have not yet been completed.
- Production AdMob IDs, signed release AAB, Play Console declarations and internal testing remain outstanding.

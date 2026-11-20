# CraftCV — feature audit and Android device verification

Updated: 19 November 2026. Android branch: `feature/native-offline-craftcv`; online app: `craftcv-hees13`.

**Status rule:** A successful build, unit test, or web deployment QA is not proof that every interaction works on a physical Android phone or with a live user account.

## Mobile layout fixes — 19 November 2026

The user supplied Android screenshots with three specific issues. The source fixes are now committed on the native development branch and the online app has been redeployed:

- **Online Studio home screen:** index viewport now includes `viewport-fit=cover` and `interactive-widget=resizes-content`; Android WebView explicitly honors the device-width viewport and does not shrink a desktop page with overview mode.
- **Online Studio mobile CSS:** final responsive overrides are placed after the original stylesheet rules so earlier desktop / sticky-footer styles cannot override them. Mobile home cards use a readable two-column layout; builder forms use one column and full width.
- **Native Education & Skills step:** short form content is vertically centered in the available workspace instead of looking stuck at the top with all unused space below.
- **Native editor keyboard:** the Previous / Continue footer hides while the Android IME is visible and returns when the keyboard closes. This prevents the fixed navigation from covering multiline work-experience fields.
- **Native Work Experience:** replaced the single long text field with a repeatable structured editor for job title, employer, industry, employment type, location, start/end dates and achievements. Existing text is migrated into a legacy entry and the structured data is mirrored back for existing PDF/DOCX exporters.
- **Native Projects & Portfolio:** added repeatable academic, personal, professional, freelance, open-source and research project records with role, organization, technologies, dates, URL and description; existing text is migrated and mirrored for exports.
- **Native finishing sections:** added certifications, languages, awards, interests and references to the final builder step and included them in PDF and editable Word output.
- **Native Education & Skills:** expanded qualification presets and the skill category library to cover IT, programming, data, cybersecurity, cloud, business, finance, creative, healthcare, engineering, education, languages, logistics and more.
- **Online Studio CV builder:** expanded the education list and skill suggestions, added work industry/employment type/location fields, project category/tools/link fields and optional references. The added fields flow into the live CV preview and Word export.
- **Online AI assistant:** added a project-description mode, mode-specific input validation and clearer provider timeout/quota/failure messages. The original CV remains unchanged until the user applies a suggestion.

The Online Studio deployment is ready and its latest QA snapshot reports no frontend or network errors. The Android code changes passed CI in run `44474517094`:
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- Debug APK artifact upload

**Still required:** repeat the same flows on a physical Android phone after installing the new debug build. CI cannot prove keyboard insets and WebView sizing behave identically on every device.

## Current verification

| Capability | Implemented in source | Automated / deployment checks | Still pending |
|---|---|---|---|
| Native dashboard and separate screens | Yes | Android compile + lint passed | Phone layout, navigation and system Back |
| Four-step offline CV builder and autosave | Yes | Compile + lint passed | Enter, save, relaunch and reopen on phone |
| Native Education & Skills editor | Multiple qualifications, expanded Pakistani/international qualification presets, institution/field/dates/grade/notes, 28 skill categories and custom categories; legacy text mirrored for exports | Android CI run `44477070888` passed before the later integrated experience/project/finishing commits; latest CI pending | Add/edit/remove multiple entries and verify save/reopen on phone |
| Saved CV library, duplicate and delete | Yes | Compile + lint passed | Persistence and original-preservation test |
| Native templates | 12 styles | Compile + lint passed | Compare actual rendered PDF styles on phone |
| Online template gallery | 14 styles and separate hash-routed pages | Latest AppDeploy deployment ready; no frontend/network errors | Real mobile interaction and browser Back |
| Profile photo | Android picker, local image and PDF inclusion | Compile + lint passed | JPG/PNG, orientation and PDF test on phone |
| Native PDF preview/export/share | A4 PDF generation, renderer, Android Save As and open/share | Compile + lint passed | Long multi-page PDF, external viewer and print test |
| Native DOCX export | Editable Office Open XML ZIP document | JUnit verifies ZIP parts, XML escaping and content | Open in Microsoft Word / Google Docs on Android and desktop |
| Cover letter | Offline editor and PDF export | Compile + lint passed | Save/reopen/export test on phone |
| Native Projects & Portfolio | Repeatable project records with category, role, organization, tools, dates, URL and achievements; legacy mirror keeps existing exports compatible | Latest integrated Android CI pending | Add/edit/remove multiple projects and verify PDF/DOCX on phone |\n| Native finishing sections | Certifications, languages, awards, interests and references are added to the final step and PDF/DOCX exporters | Latest integrated Android CI pending | Verify each section in PDF and DOCX on phone |\n| Native AI Assistant | Optional HTTPS summary, experience bullets and job-description review; mode-specific input validation and readable provider error messages; apply only after confirmation | Latest Android CI run `44477825474` passed unit tests, lint and debug APK build | Live AI request, 429 behavior and phone test |
| Cloud Account | Separate native entry opens authenticated Online Studio | Compile + lint passed; cloud APIs require auth | Live sign-in, create/edit/delete and cross-device test |
| Native-to-cloud sync | Not implemented | Not applicable | Offline native CVs remain local; manual online upload required |
| Online Studio | Optional WebView and browser; separate Build, Templates, Finished CV, AI and Cloud pages; expanded education and skills; structured work categories and project metadata; optional references | Latest AppDeploy status ready; frontend/network errors empty | WebView login, Android file picker and blob downloads on phone; exercise new AI modes with real inputs |
| Debug AdMob banner | Google demo IDs, one labelled adaptive banner on Dashboard only, UMP consent gate | Compile + lint passed | Actual demo impression, consent and privacy-options test |
| Production ads | Disabled unless owner supplies valid AdMob App ID and banner unit ID at release build | Build configuration reviewed | Owner IDs, AdMob app review, privacy policy, Play declarations |
| Play Store package | Workflow supports signed release AAB on manual dispatch | Latest CI run generated debug APK only | Signing secrets, signed AAB, internal testing and Play review |

## Latest CI evidence

GitHub Actions run `44477567710` completed successfully for the integrated Work Experience, Projects & Portfolio, Education & Skills, finishing sections, PDF/DOCX export and launcher icon changes. It passed `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` and uploaded the `craftcv-debug-apk` artifact (6.7 MB). The latest native AI error-message patch was included in run `44477825474`, which passed `:app:testDebugUnitTest`, `:app:lintDebug` and `:app:assembleDebug`; debug APK artifact uploaded. The older run `44255422648` also completed successfully after the original Education & Skills editor was added. The job passed:
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
- The AI endpoint accepts only the requested role, summary, skills, experience notes and optional job description from the native screen; it does not send the name, email, phone or photo. A shared compare-and-swap quota now caps the service at 100 requests per UTC day. The route remains unauthenticated and the limit is global rather than per user; verify 429 behavior and decide whether stronger per-user protection is needed before production.
- Cloud resume CRUD routes use authenticated requests and user-scoped storage. Native offline resumes do not automatically sync to cloud.

## Required physical Android acceptance test

**Not yet completed.** CI and AppDeploy browser QA are not substitutes for installing the latest debug APK and testing on a real Android phone.

1. Back up existing CVs. Install the debug update over the existing debug build if possible; do not uninstall first.
2. Create a CV and fill all four builder steps. Navigate Back/Continue, close the app, reopen and confirm autosave.
3. Open Work Experience. Add two roles with different industry categories, employment types, locations, dates and achievements. Edit/remove one entry, return to the builder, reopen the screen, and verify it persists.
4. Open Projects & Portfolio. Add an academic project and a personal project with role, category, tools, date, URL and outcomes. Edit/remove one and verify the data is retained.
5. Open Education & Skills. Add at least two qualifications (for example Intermediate and Bachelor's), then add Programming & Development, Finance & Accounting and Languages groups. Edit/remove one entry, save, return to the builder, reopen the screen, and verify it remains. Check that both PDF and DOCX show the grouped content.
6. Open Templates and apply at least four different styles. Confirm the actual PDF design changes.
7. Import JPG and PNG portraits; verify orientation and PDF inclusion/removal.
8. Open Finished CV; confirm a nonblank A4 preview, including long multi-page content.
7. Save PDF and DOCX using Android's document picker. Open both in external compatible apps; share the PDF.
10. Duplicate a CV, edit/delete the copy, and confirm the original remains unchanged after restart.
11. Create and reopen a cover letter; export and open its PDF.
12. Turn internet off and repeat native editing, templates, PDF and DOCX. These offline workflows should remain available.
13. Turn internet on. Test AI summary, bullets and job matching; test a service failure and verify CV data is not changed unless the user applies a suggestion.
12. Sign in to Cloud Account, create/edit/delete an online CV and open it on another browser/device. Confirm local offline CVs are not represented as synced.
13. Verify the debug demo banner and UMP consent/privacy choices. Confirm no ad appears in editor, preview or exports.
14. Test on a small Android screen and system Back from every page.
15. Only after all device checks pass: configure signing and AdMob, build signed AAB, complete privacy/ads declarations and run Play Console internal testing.

## Known limitations / release blockers

- Native DOCX is editable text and does not reproduce every artistic PDF theme or include the profile photo.
- Native offline CVs do not automatically sync with the cloud account.
- AI and Cloud require internet; the AI route needs abuse/cost protection before public launch.
- Android device tests and live account tests have not yet been completed.
- Production AdMob IDs, signed release AAB, Play Console declarations and internal testing remain outstanding.

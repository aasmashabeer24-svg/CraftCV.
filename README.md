# CraftCV — Android native-first project

CraftCV now has an **offline native Android resume editor** as its launcher screen.

## PDF, Word, AI and Cloud pages (Nov 17, 2026)

- Native Dashboard now has direct **PDF & Word**, **AI Assistant** and **Cloud Account** tiles in separate workflows.
- **PDF & Word:** Finished CV generates a searchable A4 PDF and an editable DOCX entirely offline. After Save As via Android's document picker, the app reopens the selected URI to check PDF header/DOCX package components when the provider permits it; **Open saved file** launches a compatible phone viewer for a real-world check. This is an in-app verification aid, not evidence of device testing already completed.
- **Native AI Assistant:** Optional online screen uses the existing CraftCV /api/resume-assistant backend (summary rewrite, experience bullets or job comparison). Only role, summary, skills, work-experience notes and optional job description are sent over HTTPS after an explicit **Generate** tap; name, phone, email and portrait are not sent. The user can copy results or explicitly confirm applying an AI summary to the local CV. AI may be unavailable when the AppDeploy backend is paused or at its service limit.
- **Cloud Account:** Dedicated Android entry screen opens the existing authenticated Online Studio cloud page. Login may need a system browser; an external-browser option is provided. **Offline native CVs are NOT automatically synced to the online account**—the existing website only syncs CVs created/uploaded in its own workflow. Neither native cloud authentication nor native cloud syncing is claimed.
- AI and Cloud need internet. Native PDF/DOCX and offline CV editing still work without it.
- New cloud/AI/export UI is on the draft Android development PR until real phone testing; no signed Play Store AAB or Google Play upload.

## Mobile navigation redesign (Nov 17, 2026)

- Dashboard: dedicated landing page with Create CV, Continue Editing, Saved CVs, Templates, Cover Letters, Finished CV and optional Online Studio.
- Build My CV: a **dedicated four-step builder** (Personal Details, Profile & Experience, Education & Skills, Projects & Finish), progress indicator, fixed Previous / Continue buttons and local autosave.
- My Saved CVs: a separate list with per-resume Edit, View, Copy and Delete actions.
- CV Templates: a separate visual gallery. Existing CV details are retained when applying a style.
- Finished CV: separate A4 page, PDF, Word and sharing options. Cover letters remain their own separate page.
- Existing offline CVs retain their data keys and can be reopened without migration.
- These Android changes are on the **draft GitHub development branch**, not the existing AppDeploy web site or Play Store.

## What's built on this development branch

- Native Android UI that works when AppDeploy and the internet are unavailable.
- Locally saved resumes with a resume switcher and multiple CVs.
- Edit personal details, summary, experience, education, skills, and projects.
- Optional profile photo import using the Android image picker. Photos are compressed into app-private storage and included in native PDF previews/exports; native DOCX currently remains a text-focused editable export.
- 12 native CV styles with different header treatments, colors and typography (not yet 12 wholly distinct page layouts), visual gallery, and on-device A4 preview.
- **Separate finished CV preview page** using Android PdfRenderer: visually displays the exact A4 pages generated for export.
- Android-native PDF export with width-based wrapping, multiple pages, Save As document picker, and a Share PDF action for compatible apps.
- Online Studio WebView: Android Save as PDF/Print integration and a blob-to-document bridge for Word exports.
- Native editable DOCX export using an offline WordprocessingML writer, saved to a user-selected file.
- Duplicate/delete multiple local CVs with confirmation and automatic local saving.
- Student/fresh-graduate starter and clear resume-completeness checklist (not an ATS guarantee); autosave when editing.
- Offline editable cover-letter maker, manual draft saving, and cover-letter PDF export.
- The previous advanced CraftCV website remains available as an **optional online studio**.
- No AdMob SDK or test ad banners inside the Android app.
- Android 16/API 36 target and GitHub Actions lint/debug build workflow retained.

## Architecture and limitations

This is a **phased migration**, not a claim that every feature is offline or native yet. The native editor is independent of AppDeploy for the tasks above. Advanced AI writing, online cloud accounts and advanced website designs are still features of the optional web studio, and may be affected when AppDeploy is unavailable. Native DOCX export is a basic editable document and does not reproduce artistic themes.

The new finished CV page, image import, website WebView print/download bridge, and native CV PDF/DOCX and cover-letter exporters need physical-device verification (including Unicode text, file opening, multi-page documents and sharing to messaging apps). The online AppDeploy website can still show provider pause screens when opened through the optional online studio.\n\nOffline resume data lives in Android app-private storage. It is not automatically synced with web accounts. It is not yet encrypted with an app-specific encryption layer. Uninstalling or clearing app data can erase local CVs; export PDFs and keep copies.

An Android App Bundle (AAB) must still be built with a **private signing key**, tested on real devices, and reviewed before Google Play release. Do not commit keystores or secrets.

## Build

1. Open the repository in Android Studio (JDK 17).
2. Let Gradle sync with Android SDK 36 installed.
3. Run `./gradlew :app:assembleDebug` if you have the Gradle wrapper available, or `gradle :app:assembleDebug` with a compatible Gradle installation.
4. Install the debug APK on a phone. The launcher opens `DashboardActivity`.
5. On the dashboard, open **Build My CV**, complete four steps, then check the full-page PDF preview and save or share PDF/Word.
6. Open **Online Studio**. Check **Save PDF**, website **Download PDF** (native print dialog), and **Download Word** (Android Save As file picker).
7. Test the separate Saved CVs and Templates pages, saved-record retention, duplication, deletion, fresher starter, cover letters and offline mode.
8. Use **Open advanced web studio** if online advanced functionality is needed.

The CI workflow performs `:app:lintDebug` and `:app:assembleDebug` on pushes and pull requests. Manual signed AAB builds require signing secrets.

See `docs/PLAY_CONSOLE_CHECKLIST.md` and `docs/RELEASE_SIGNING.md` for remaining release requirements.

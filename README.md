# CraftCV — Android native-first project

CraftCV now has an **offline native Android resume editor** as its launcher screen.

## What's built on this development branch

- Native Android UI that works when AppDeploy and the internet are unavailable.
- Locally saved resumes with a resume switcher and multiple CVs.
- Edit personal details, summary, experience, education, skills, and projects.
- Editorial, Modern, and Creative color themes with an on-device text preview.
- Android-native PDF export using the system Save As document picker.
- The previous advanced CraftCV website remains available as an **optional online studio**.
- No AdMob SDK or test ad banners inside the Android app.
- Android 16/API 36 target and GitHub Actions lint/debug build workflow retained.

## Architecture and limitations

This is a **phased migration**, not a claim that every feature is offline or native yet. The native editor is independent of AppDeploy for the tasks above. Advanced AI writing, online cloud accounts, advanced website designs, and DOCX export are still features of the optional web studio, and may be affected when AppDeploy is unavailable.

Offline resume data lives in Android app-private storage. It is not automatically synced with web accounts. It is not yet encrypted with an app-specific encryption layer. Uninstalling or clearing app data can erase local CVs; export PDFs and keep copies.

An Android App Bundle (AAB) must still be built with a **private signing key**, tested on real devices, and reviewed before Google Play release. Do not commit keystores or secrets.

## Build

1. Open the repository in Android Studio (JDK 17).
2. Let Gradle sync with Android SDK 36 installed.
3. Run `./gradlew :app:assembleDebug` if you have the Gradle wrapper available, or `gradle :app:assembleDebug` with a compatible Gradle installation.
4. Install the debug APK on a phone. The launcher opens `NativeResumeActivity`.
5. Test creating, saving, reopening, editing, and exporting a CV.
6. Use **Open advanced web studio** if online advanced functionality is needed.

The CI workflow performs `:app:lintDebug` and `:app:assembleDebug` on pushes and pull requests. Manual signed AAB builds require signing secrets.

See `docs/PLAY_CONSOLE_CHECKLIST.md` and `docs/RELEASE_SIGNING.md` for remaining release requirements.

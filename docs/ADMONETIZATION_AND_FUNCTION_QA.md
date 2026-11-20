# CraftCV – functional QA and safe AdMob launch

Audit updated: November 17, 2026. Development branch: `feature/native-offline-craftcv`.

## Feature confidence

| Feature | Source implementation | Automated verification | Still requires device/account checks |
|---|---|---|---|
| Dashboard | Offline native Activity with dedicated routes | Android debug compile + lint | UI cards and Android Back, phone sizes |
| Build My CV | Four native wizard steps and autosave | Compile + lint | Input across all steps, reopen after relaunch |
| Saved CVs | Private SharedPreferences JSON records, duplicate/delete | Compile + lint | Original survives duplication/deletion and restart |
| Templates | 12 preview styles and PDF themes | Compile + lint | Actual visual differences on phone, persistence |
| Profile photo | Android content picker, local compressed JPEG and PDF include | Compile + lint | JPG/PNG selection and rotation with real photos |
| PDF preview/export | Offline generated A4 PdfDocument and PdfRenderer, SAF Save As + share | Compile + lint | Long multi-page PDF, open with viewer, printing/sharing |
| DOCX export | Offline ZIP-based WordprocessingML, SAF Save As | JUnit confirms ZIP parts, XML escaping and content | Open in Word/Docs on Android and desktop |
| Cover letter | Offline editor and PDF export | Compile + lint | Reopen, export, verify text |
| AI suggestions | Explicit HTTPS call to existing AppDeploy backend, optional apply | Compile + lint | Network response, errors, service credits and consent |
| Cloud account | Dedicated native link to authenticated website | Compile + lint | Browser/in-app sign-in and online storage; no native autosync |
| Online Studio | Existing AppDeploy web UI in optional Android WebView | Prior web deployment QA | Web login, file picker and blob downloads on a phone |
| Home banner | AdMob + UMP consent-gated test banner in debug only | Compile + lint | Actual test ad impression, consent UI and privacy choices |
| Production ads | Release gated by validated owner AdMob app/banner IDs | Build flags and manifest replacement | Owner IDs, approved app, privacy policy and AdMob review |

**Compilation and JUnit do NOT establish that all features are working on real devices.**

## AdMob configuration

Create/register CraftCV in [AdMob](https://admob.google.com/), then create a **Banner** unit.

- Android application ID/package: `com.craftcv.resumestudio` (the debug app uses a `.debug` suffix).
- Set `CRAFTCV_ADMOB_APP_ID` to the publisher *application* ID (`ca-app-pub-################~##########`).
- Set `CRAFTCV_ADMOB_BANNER_UNIT_ID` to the publisher *banner* unit (`ca-app-pub-################/##########`).
- For a signed GitHub release build, provide these values as repository secrets in the workflow's environment (not in committed files). The workflow has to expose secrets as environment variables during Gradle execution before live ads are enabled.
- **Debug builds** use Google's official demo App ID and demo banner ad unit. They are TEST ads and do not earn money.
- **Release builds without both verified-format non-demo IDs** have `BuildConfig.ADMOB_ENABLED=false`: no banner requests or revenue; releases stay otherwise functional.
- Ads are requested only after UMP's `canRequestAds()`. The app offers a privacy-options button if UMP says it is required.
- Create/activate relevant Privacy & messaging consent messages in AdMob before production. Ensure proper age audience and region handling. Review privacy policy and Google Play Data safety and **Contains ads** declarations. The embedded SDK may include Advertising ID permissions even in a build with ads disabled.
- Place only one clearly labeled banner below the dashboard; no interstitial during editing, on download screens, or inside PDF/Word output.
- Before publishing live ads, test using Google's test IDs and test-device configuration. Never click your own live ads.

## Suggested real-phone acceptance checklist

1. Launch with internet off. Open Build CV, enter all details, use Back/Continue, relaunch, verify autosave.
2. Open Templates and switch at least four styles. Confirm real PDF differences.
3. Select/import portrait photo; verify orientation and resulting PDF photo.
4. Open Finished CV; check nonblank A4 preview and **Save PDF**; choose a folder and tap **Open saved file** to verify a PDF reader opens it.
5. Save DOCX, verify in-app ZIP structure check and **Open saved file** using Microsoft Word/Google Docs.
6. Duplicate and delete an unused CV; verify the original remains intact.
7. Export a cover letter PDF; open it.
8. With internet enabled, use AI Assistant: summary, bullets, job matching; check errors and explicitly apply a suggestion. Confirm no name/contact/photo was uploaded.
9. Sign in to Cloud Account via browser and save/open an online CV on another browser; check offline CVs remain local.
10. In **debug**: verify visible 'ADVERTISEMENT' label and demo banner, verify no ad on editor, CV preview or download pages. Test consent and privacy-options UI where applicable.
11. On a small Android phone, test no blank screen, CV word wrapping, all cards scroll, system back, portrait image picker.
12. Only after all pass: build signed AAB, update public privacy policy, fulfill Play Console ads/Data safety declarations and run internal/closed testing.

## Limitations

No production ad revenue is configured until the owner supplies actual IDs. Native offline CVs are not automatically synced to Online Studio. AI backend requests depend on existing online service availability. User authentication was not inspected with a live test account. Real Android device testing remains pending.

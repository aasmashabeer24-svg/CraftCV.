# CraftCV — feature audit and Android device verification

Updated: 17 November 2026. Source: Android branch `feature/native-offline-craftcv` and deployed AppDeploy site `craftcv-hees13`.

**Do not interpret a successful build or screenshot QA as proof that exports and all user interactions work on a physical Android phone.**

## Implemented and source-reviewed

| Capability | Android native | Online website | Verification |
|---|---|---|---|
| Dedicated Home, Build, CVs, Templates screens | Dashboard + separate activities | Hash routes for each workflow | Source reviewed; Android compilation/lint and web build/QA passed |
| Builder steps | 4 step panels with progress and autosave | 7 conditional steps inside dedicated Build page | Source reviewed; manual step navigation pending |
| Saved CVs / duplication / deletion | Private JSON records, confirmation | Browser localStorage library, confirmation | Code reviewed, manual persistence test pending |
| Templates | 12 styles; fixed squared-dp sizing bug; PDF header treatments | 14 styles, visual gallery, live actual CV | Source reviewed; device visual comparison pending |
| Optional profile photo | Local photo picker, compressed private file, native PDF | Crop tool; PDF + Word | Code reviewed/build passed; actual camera/gallery test pending |
| Finished A4 CV | Background PDF renderer to avoid blank UI; save/share | Dedicated preview screen and Print as PDF | Source reviewed; physical-device printer test pending |
| DOCX | Offline, editable text-based DOCX; selected photo not yet embedded | Styled DOCX including photo and ordered sections | Code reviewed, Office-compatible file test pending |
| Cover letter | Standalone editable offline letter, local draft, PDF export | Not a standalone web feature in this branch | Native code reviewed, Android test pending |
| Cloud account and sync | Only inside optional Online Studio; no native sync | Account APIs; per-user authenticated CRUD | Source reviewed, signed-in end-to-end test pending |
| AI writing + ATS helper | Optional online website only | AI backend; completeness checklist (not certified ATS score) | API present, successful live response not confirmed |
| Android back and Word blob download | Safe WebView history check; native document picker bridge | Hash routing and download anchor | Compiled; device test pending |
| Ads | No advertising SDK | No native ad integration required | Android dependencies and manifest checked |
| Google Play | Signed AAB pipeline defined | N/A | No signed Play release or internal-testing upload verified |

## Repaired during this audit

- `ResumeTemplatesActivity.dp()` incorrectly multiplied by `n` twice. With normal phone density, padding widths grew far beyond the screen. Corrected to standard density conversion.
- A stale selected resume ID no longer prevents a template from applying to a real saved CV.
- Disabled Save PDF / Share in the native preview until the actual PDF bytes have been generated.
- Made Online Studio toolbar Back use the safe WebView back navigation, and clarified the standalone preview Back label.
- Added native photo import and PDF rendering; fixed photo downsampling initialization to prevent division by zero.
- Online Word export now carries accent colour, optional photo and current section order; mobile Split Column preview retains its column layout.

## Required physical Android test — run in this order

1. **Data safety:** Export existing CVs first. Install the new debug APK over the old debug APK where possible; do not uninstall before backing up.
2. **Dashboard and builder:** Open Home, create a CV, enter a name, phone, email, summary, education and skills across all four steps. Close and reopen. Confirm text remains.
3. **Gallery:** Test Modern, Creative, Editorial and at least one other design. Ensure card sizing is normal and preview genuinely changes.
4. **Photo:** Pick a JPG/PNG from phone, check upright orientation, view the PDF with photo, remove the photo, view again.
5. **Finished CV:** Observe a visible loading state, then an actual full-page A4 preview, not blank white. Try long text to produce multiple PDF pages.
6. **Files:** Save PDF and Word using Android's document picker. Open both in a separate compatible app and check content. Share the saved PDF.
7. **Saved CVs:** Duplicate, edit the copy, reopen the original, delete the copy with confirmation; verify the original remains intact.
8. **Cover letter:** Generate starter, replace placeholders, save locally, leave/reopen, export/open PDF.
9. **Offline:** Turn off internet and repeat native editing, template selection and PDF/Word generation.
10. **Online Studio:** With internet, navigate Home → Build → Templates → Finished CV → Back; verify no white screen. Use its Word and PDF buttons. Test AI and Cloud separately after signing in; these require service availability.
11. **Android back:** Test both phone system Back and top-left Back from each of the above pages.
12. **Play readiness:** When all pass, create a signed release AAB with private signing secrets and test an internal Play Console release. No user credentials should be shared in chat.

## Remaining limitations — not claimed finished

- Native Word DOCX is editable but does **not** duplicate every artistic PDF theme or include the profile photo.
- Advanced AI and cloud accounts need internet and are only exposed through the optional Online Studio.
- The offline Android CV database does **not** automatically sync to the online browser account.
- A real-device pass is required; CI compiling Java and AppDeploy page-load QA are not end-to-end proof.
- Google Play release signing, console declarations and real internal testing remain outstanding.

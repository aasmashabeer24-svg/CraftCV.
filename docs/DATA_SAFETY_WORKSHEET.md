# CraftCV Data Safety — verification worksheet (not final declaration)

This is a research worksheet, NOT a completed Google Play Console declaration. Answers must be verified against the actual production AppDeploy authentication, hosting and AI services.

## Data the current CraftCV workflows may process
- Personal info: user names, email addresses, phone numbers, city/location written in CVs, photographs.
- Professional data: resume experience, education, skills, portfolios, cover-letter content.
- Files: user-uploaded portrait images and resume documents.
- Authentication: user IDs, email and session details via the AppDeploy identity system.
- Analytics/crash info: determine whether the hosting / app / Android libraries collect any.
- AI service: resume content or job descriptions may be sent to the configured backend/AI provider.
- Cloud synchronization: resumes and related metadata stored for logged-in users.
- Local content: drafts, preferences and styles may remain in browser/WebView storage.
- Google Play's Data Safety data types and collection/sharing/retention flags must be assessed individually.

## Publisher confirmations required
1. Is any ad SDK, analytics SDK, crash reporter or push service enabled?
2. Which AI provider(s), server region(s), subprocessors and retention windows apply?
3. Is data encrypted in transit (TLS) and at rest by all providers? Confirm documentation.
4. Can a user permanently delete the account and associated server/provider data?
5. Which data is optional vs required? Which data is shared with third parties?
6. Is any data used for training or advertising? Verify with providers.
7. Does the service target children, or include any child-directed features?
8. Does the native WebView request permissions beyond INTERNET and network state? Recheck final manifest merger.

Do not guess answers for Google Play; falsely answering 'No data collected' may violate policy.

## AdMob update (2026-11-14)
- Android app now includes Google Mobile Ads SDK 25.5.0 and UMP SDK 4.0.0. The actual AdMob App ID is in AndroidManifest.xml.
- Google official test banner only loads in DEBUG builds after UMP canRequestAds() returns true; RELEASE builds intentionally do not show ads yet.
- The supplied ad unit `ca-app-pub-3517751367362331/3094894516` is for **partner bidding** and must NOT be passed as a standard banner ID. Create a Standard Banner unit to use direct AdMob, or provide details for a supported third-party mediation service.
- Before production, configure Privacy & messaging in AdMob, implement privacy-options access as required, verify SDK / consent behavior, update the public privacy policy and Play Data Safety, and disclose "Contains ads" if ads are enabled.

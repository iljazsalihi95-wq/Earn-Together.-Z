# Authentication diagnostic — 24 October 2026

Scope: email/password requests, Google authentication request lifecycle, existing backend. No GamePix, native-game, LazyGarden, payment or ad configuration changes.

## Live results against the existing deployed backend

- GET health: HTTP 200 JSON, version 3, 9.10 s.
- POST googleChallenge: success, 8.82 s.
- POST invalid-email login: LOGIN_FAILED JSON, 16.34 s.
- POST register with a generated diagnostic identity: success, 16.87 s.
- POST logout: success, 10.22 s.
- POST login with the same diagnostic identity/password: success, 24.96 s.
- POST validateSession: success, 9.97 s.
- POST getProfile: success, 9.46 s.
- POST logout: success, 10.75 s.
- The diagnostic user's USERS row was independently read back through Google Sheets.
- No real user's password was accessed or changed. Diagnostic tokens/passwords were not committed or logged.

The existing deployment accepts POST JSON, redirects its JSON response to Google ContentService, and accesses the existing private database. Native HttpURLConnection is not subject to browser CORS. No deployment URL, Apps Script source, permissions or database replacement was necessary based on these live tests. The Apps Script editor was also opened and its source reviewed.

## Android defects addressed

The previous client permitted 150-second read timeouts on each redirect hop and had no overall request/UI deadline. Authentication also shared a single serial executor with queued game-save work. Google provider startup could throw synchronously without restoring the UI. These defects can cause prolonged or unbounded-looking loading. The exact user's APK #230 device stall has not been reproduced on a physical device.

- Separate serial authentication executor; game-save queue remains unchanged.
- Overall 60-second HTTP deadline across redirects, 15-second connection/45-second read limits, plus active connection disconnection on deadline.
- Preserve POST for 307/308; change to GET only for 301/302/303 as required by ContentService.
- UI timeout at 70 seconds for backend work, 120 seconds for the Google account picker; controls are re-enabled and stale UI callbacks ignored.
- Explain that a timed-out registration may already exist and suggest signing in, rather than promising automatic retry or duplicate creation.
- Preserve useful backend errors and catch synchronous Google provider failures.
- Existing post-login cloud-save restoration, account isolation and conflict decisions remain unchanged.

## Google and device limitations

Live googleChallenge success is not Google login success. Generated RSA-token tests pass, but no real Google ID-token exchange from the Android account picker was exercised here. Native Google login requires the installed APK's signing SHA-1 to match the existing Android OAuth client, the correct web audience, enabled Google Play services and permitted consent/test users. CI may use a different debug certificate unless persistent signing secrets are configured. No OAuth security settings were weakened and no fake Google success was substituted.

A new APK build must be checked on Android for registration/login navigation, Google picker and entry to GAMES > GAMEPIX CATALOG. Compilation alone is not that verification.

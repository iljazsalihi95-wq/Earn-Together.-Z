# We Earn Together cloud backend

Android native email/password accounts use the deployed Google Apps Script JSON API:

https://script.google.com/macros/s/AKfycbw4GAz9_Om3YVU1Eil30WgEHvFkvW3ygo4sGqXNG8QBOGitUXGOkXWbntpJO4JybXPr/exec

Administrator spreadsheet: `1UNsw9fVccM92q1PEbS9FUqtWpB1Em8FPFX-sTH4c_68`.
Script project: `1050vlw5qdMi3hFt7oQNcSkOPEK332Z4rDXFKk_jrwTAEMXVeu_59CzSG`.
The Sheet remains private. Web App access is Anyone; protected API operations require a valid session.

## Accounts and sessions

- POST JSON actions: register, login, logout, validateSession, getProfile, updateProfile, saveProgress, getProgress, syncProgress.
- Register validates name, normalized unique email and password on the server. Account creation uses a script lock.
- Passwords use salted PBKDF2-HMAC-SHA256 with 600,000 iterations. Only the hash and salt are stored.
- Only token hashes are stored in SESSIONS. Sessions expire after 30 days and logout revokes them.
- Android encrypts its session token with device-bound Android Keystore AES-GCM. Backup is disabled in the manifest.
- No passwords, tokens, salts or hashes are displayed in Profile or logged by the client.
- All network operations run on one serial background worker with loading and real errors. Google response redirects are restricted to HTTPS script.google.com/script.googleusercontent.com.
- Cached, unexpired sessions can continue with explicitly labeled offline device saves if validation is temporarily unavailable. Server rejection returns the player to sign-in.

## Progress and migration

- LazyGarden, Block Puzzle and Bubble Shooter sync complete existing preference state, including boards, boosters, scores, completed/restored tasks and garden gameplay coins. Garden gameplay coins have no monetary value.
- The first online account claims a COPY of legacy device progress; originals remain intact. Other accounts get separate preference stores. Old local password hashes are not cloud credentials: users create a real online account with their chosen email/password.
- One GAME_PROGRESS row per server-derived user + game. The client never supplies an authoritative user ID.
- Saves use a server `updated_at` as an optimistic version (`base_updated_at`). Stale saves return conflict instead of overwriting.
- Unchanged device saves can restore newer cloud progress. Offline changes upload when the remote version is unchanged. Divergent saves show the levels and ask the player to choose. Both copies are backed up before a choice; version is checked again when saving.
- Sync occurs at sign-in/session restore, when returning to the hub, on app/game stop, and through Profile's Sync button. Local saves remain available offline.

## Economy and Google sign-in

WALLET_LEDGER and AD_EVENTS are administrative schemas only. No money, EUR conversion, fake ad rewards or payouts are issued. The existing LevelPlay integration is unchanged.

GOOGLE LOGIN BLOCKED — OAuth Client ID required. There is no Android OAuth configuration in this repository. To enable it, create an Android OAuth client for `com.weearntogether.play` with the actual installed build signing SHA-1/SHA-256, plus the server/web OAuth client used to verify ID tokens. Configure the consent screen and test users. Supply client IDs (not client secrets in the APK); then implement server-side ID-token audience/issuer/expiry validation and account linking. Email/password works independently.

## Verification

2026-10-22: real deployed endpoint tested for registration, login, session validation, profile read/update, saves/reads of all three games, UPSERT, stale-version conflict, duplicate email rejection, wrong password and invalid token rejection, logout and post-logout rejection. Physical USERS and GAME_PROGRESS rows were read back from Google Sheets. Test identities are disabled after verification. CI tests the save conflict policy and builds the real APK artifact; compilation alone does not establish device runtime behavior.

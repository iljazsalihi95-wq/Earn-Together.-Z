# Google Sign-In

Cloud project: `we-earn-together` (726413248892), name **We Earn Together**.

Public OAuth identifiers supplied by the owner:

- Web/server audience: `726413248892-hvdca11ahhbb1lm6in5sdviglep69494.apps.googleusercontent.com`
- Android: `726413248892-gbdnvfkep8f9ro7p05ivktnef1a93a1p.apps.googleusercontent.com`
- Application ID: `com.weearntogether.play`

No client secret belongs in this repository or Android. The native Credential Manager button requests a Google ID token for the Web audience with a one-time server nonce. Google tokens are held in memory and exchanged for the existing backend session; AndroidKeyStore protects that session as before.

## Existing Apps Script extension

`backend/GoogleAuth.gs` extends the existing script: rename the original `route_` to `emailRoute_` and add the extension once. It adds `googleChallenge`, `googleSignIn`, and `linkGoogle`. Keep the existing Sheet, Script Properties, and deployment URL. No setup/recreation is required.

Verification checks Google's HTTPS-fetched public signing keys, exact RS256/PKCS#1 v1.5 SHA-256 signature, issuer, audience, authorized party when present, expiry, issued-at/not-before, verified email and subject. It consumes a five-minute nonce under the script lock. No caller-provided keys or unsigned token claims are trusted. Google JWKS are cached five minutes; an unrecognized rotated key fails closed and the player can retry.

New users get provider GOOGLE and blank password columns. Returning Google users are located by `sub` in `google_id`, retaining the existing user ID and saves. Matching email alone never links an existing account. Sign in with its password, open Profile, then select LINK GOOGLE ACCOUNT with the same Google email; both the existing session and a fresh Google proof are required. An already-linked Google identity cannot move to another player. Linking preserves the existing password hash/provider so email/password keeps working.

## Signing requirement

The Android OAuth client created by the owner initially uses the certificate from Actions APK #156:

- SHA-1 `43:70:A3:05:C3:A6:71:E7:F7:AB:96:F8:BD:D9:08:5A:9C:6B:D3:57`
- SHA-256 `C7:4B:17:12:A2:62:B1:9D:05:D7:51:4F:DC:B3:1C:D7:EC:19:52:46:8E:D5:26:C3:5C:DE:63:BD:F7:CF:6A:A0`

Default GitHub-hosted runner debug keys change between builds. This certificate does **not** establish persistent signing for subsequent APKs. The workflow supports these repository Actions secrets for a persistent owner-controlled keystore:

- `WET_SIGNING_KEYSTORE_BASE64`
- `WET_SIGNING_STORE_PASSWORD`
- `WET_SIGNING_KEY_ALIAS`
- `WET_SIGNING_KEY_PASSWORD`

Private keys must never be committed, printed in logs, or cached in public Actions caches/artifacts. With secrets configured, CI restores the keystore only to the temporary runner directory and Gradle uses it. `signingReport` publishes only public certificate fingerprints. Register that exact SHA-1 in the Android OAuth client. Google Play builds need a client for the **Play app signing certificate**, not just the upload certificate.

In Google Auth Platform testing mode, add the intended test accounts under Audience / Test users. Device testing requires Google Play services and an APK signed by the registered certificate. A compilation or mocked token test is not evidence that the Google account picker or actual user authorization succeeded.

## Verification

Run `node backend/tests/google-auth.test.cjs` for generated RSA signature verification, tampered/invalid/expired tokens, issuer/audience/azp, nonce replay, disabled accounts, duplicate-email protection, authenticated linking, stable user IDs, preservation of password hashes and legacy API dispatch. These generated test keys are never trusted by production.

Run Android unit tests and `assembleDebug` in the existing workflow. After OAuth/signing is configured, test new Google registration, returning sign-in, link an existing email account, restore/relaunch, logout, and all three games' cloud saves on an actual Android device. Report those device results separately from backend and build tests.

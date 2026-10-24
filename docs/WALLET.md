# Verified wallet and manual cash-out

Extends the existing Apps Script project and existing private spreadsheet. Authentication and GamePix routes are retained by delegation to `preWalletRoute_`. No automatic payments, fabricated revenue, fixed coin-to-EUR rate or partner share is configured.

## Tables

Reuse USERS, SESSIONS and WALLET_LEDGER. Add COIN_LEDGER, REWARD_SESSIONS, REVENUE_POOLS, CONVERSIONS, CASHOUT_REQUESTS and AUDIT_LOG. Existing user/session/password/game-progress rows are not migrated or overwritten.

All monetary fields, including REVENUE_POOLS amounts and CASHOUT_REQUESTS.amount, store **integer EUR cents**. User input amounts are decimal strings parsed without binary floating-point arithmetic. Coins are separate integers. Payment destinations remain in the private Sheet and are omitted from public history responses. They are not passed to ad SDKs or logged on Android.

## API

Authenticated: getWalletProfile, getBalance, startRewardSession, recordRewardEvent, getCoinHistory, getCashoutEligibility, requestCashout, getCashoutHistory, cancelCashout, getTransactionHistory. Tokens determine identity; caller user_id/balance/coins/status overrides are rejected. Histories paginate 50 rows at a time. Existing getProfile and all authentication endpoints remain unchanged.

Client event reporting creates PENDING or REJECTED records, never automatically VERIFIED cash. A server-issued reward session and event id are required. Default server rules propose GAME=1, LEVEL=10, ACHIEVEMENT=25 coins; AD_REWARD/GAMEPIX/BONUS=0 until independent evidence/rules are integrated. These are noncash reward claims. Native gameplay and GamePix callbacks are **not yet wired to these APIs**; no callbacks or revenue proof are invented. Independent owner verification is required. There is no device fingerprint collection. Account identity uniqueness remains enforced by the existing email/Google authentication; sophisticated multi-account detection requires additional lawful evidence.

Duplicate event IDs and repeated lifetime level/achievement keys do not credit twice. GAME events are one per reward session. Server timestamps, session ownership/expiry, minimum 10-second gameplay, minimum 5-second event interval, per-minute event limit and client-clock bounds are checked. A plausible event remains PENDING until independently verified. Invalid time intervals are REJECTED; no cash balance is created.

## Owner-only administrative functions

Run configureWalletAdmin and setupWallet from the owner Apps Script editor. No HTTP admin endpoints or admin credentials are embedded in Android. Administrative helpers are in Wallet.gs and require the configured active/effective owner identity:

- walletReviewCoins_(transactionId, 'VERIFIED' or 'REJECTED', evidenceReference, reason)
- walletAdjustCoins_(userId, positiveCoinAmount, uniqueReference, reason): audited PENDING adjustment, never direct cash issuance.
- walletCreatePool_(fields): completed, nonoverlapping period; decimal strings for gross_confirmed_revenue, player_reward_pool and funded_amount; unique revenue_reference and funding_reference; partner_approval_reference.
- walletCalculatePool_(periodId), walletApprovePool_(periodId), walletClosePool_(periodId)
- walletReviewCashout_(cashoutId, targetStatus, paymentReference, reason)

These helpers require explicit editor calls with real reviewed evidence. They do not import or verify partner revenue automatically. No production pool is seeded or approved as part of installation. Negative cash/coin corrections are not exposed by this first version; financial reversals require an additional audited policy rather than editing balances or historical conversion snapshots.

## Conversion

Only VERIFIED coins earned within [start_date,end_date) and verified by snapshot_at are eligible. Periods cannot overlap. The rate is stored as the exact rational `reward_pool_cents / eligible_coins`. BigInt arithmetic computes integer cent allocations; largest-remainder distribution with deterministic user-ID tie breaking preserves the funded pool exactly. Immutable CONVERSIONS store each user's coins, cents, rate numerator/denominator and snapshot hash. Conversion rows are not spendable until the final APPROVED pool event exists. Approval retries reuse deterministic conversion IDs; changed snapshots fail closed. CLOSED preserves the approved snapshot.

## Cash out and audit

Minimum 500 cents. A single immutable REQUESTED cash-out journal row simultaneously reserves available verified cash. The existing global script lock serializes balance check and append. Idempotency keys prevent retry duplicates; reused keys with changed details are rejected. User cancellation is allowed only before review. Admin flow: REQUESTED -> UNDER_REVIEW -> APPROVED -> PAID. Rejection before PAID releases the reservation. A PAID transition requires a unique real payment reference; the system never sends money. Only the user's own history is returned.

Coin decisions, pool changes and cash-out changes append events; previous rows remain. Audit intent is appended before the corresponding journal row. Read projections verify the audit hash chain and matching audited rows and fail closed on inconsistencies. This detects accidental/silent edits through normal operation, but a Sheet owner with full source/data access can rewrite history: an independently anchored audit service is needed for stronger tamper evidence. Sheets provides no multi-table transaction: an interrupted operation can leave an unused audit intent or unapproved conversion rows. Those do not become spendable; reconciliation is manual and idempotent. Operational scale is bounded by Apps Script/Sheets quotas.

## Android

PROFILE links to a native PROFILE wallet screen with avatar/name/email/user ID/joined date, Pending Coins, Verified Coins, Available Cash, Total Earned, Total Paid and Pending Cash Out; a €5 eligibility-gated Cash Out screen; coin, cash-transaction and cash-out histories. All balances come from the authenticated backend. Loading/error/timeout states and stale-response guards are implemented. GamePix, native games, LazyGarden and ad integration are unchanged.

## Verification boundary

`node backend/tests/wallet.test.cjs` executes the production wallet code against in-memory Script service fixtures. It tests identity isolation, duplicate events/session ownership, clock/rate checks, pending/rejected cash exclusion, funded approved-period conversion, 4.99/5.00 boundary, competing withdrawals, idempotency, reservation release, PAID history and integer rounding. Test revenue and payment references exist only in the isolated fixture, never production.

Live production tests may test zero-balance wallet/profile/history and rejection paths, plus real registration/login regression. A positive funded cash-out and real payment cannot be claimed tested without actual independently confirmed funds and payout evidence. Device UI remains subject to Android testing. Negative correction policy, trusted partner callbacks/revenue import, signed native gameplay verification and regulated-payment operational requirements remain integration work.

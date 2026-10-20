# We Earn Together — Revenue Ledger Backend Specification

## Goal
Build the real-money system around verified partner revenue attributable to each individual player. The Android APK must never be the financial authority.

Default launch policy: 70% player / 30% platform. The backend may change the policy later, but every verified transaction stores its own immutable split snapshot so historical balances never change retroactively.

## Money flow
1. User signs in and receives a stable server user ID.
2. Android registers an installation token with that user.
3. Offerwall/ad partner receives our opaque user ID as its sub/user parameter.
4. Partner sends a signed server-to-server postback for a conversion.
5. Backend validates signature/source, partner conversion ID, user ID, amount and currency.
6. `(partner, conversion_id)` is unique. Duplicate callbacks cannot create money twice.
7. Revenue enters `PENDING` until partner rules allow verification.
8. On `VERIFIED`, backend snapshots the active split and calculates cents with integer arithmetic.
9. Player share moves to `PAYABLE` only after settlement/risk rules are satisfied.
10. Automatic payout worker may send PayPal payout only when payout policy, identity and minimum threshold are satisfied.
11. Payout uses a unique idempotency/batch key. Retry must not pay twice.
12. Partner reversal creates a reversal ledger event; history is never silently edited.

## Core tables
### users
- id UUID primary key
- email/login identity
- status
- created_at

### installations
- id UUID
- installation_token_hash unique
- user_id
- first_seen_at / last_seen_at
- risk_state

A device signal is a risk input, not an infallible identity. Factory reset/cloning must not be treated as impossible.

### payout_profiles
- user_id unique
- normalized_paypal_email unique when verified
- full_name
- address fields required by compliance policy
- verification_state
- created_at / updated_at

One verified PayPal payout identity cannot be attached to multiple active reward accounts.

### revenue_events
- event_id UUID primary key
- partner
- partner_conversion_id
- user_id
- verified_revenue_cents
- currency
- status
- player_basis_points
- platform_basis_points
- player_share_cents
- platform_share_cents
- created_at / verified_at / reversed_at
- UNIQUE(partner, partner_conversion_id)

### payouts
- payout_id UUID primary key
- user_id
- amount_cents
- currency
- status
- provider = paypal
- provider_batch_key unique
- provider_reference nullable
- created_at / processing_at / paid_at / failed_at

## API contract
- `POST /v1/auth/register`
- `POST /v1/auth/login`
- `POST /v1/installations/register`
- `PUT /v1/payout-profile`
- `GET /v1/wallet`
- `GET /v1/ledger`
- `POST /v1/postbacks/{partner}` — partner server only, signature required
- `POST /v1/payouts/request` — optional if automatic payout is disabled

The public Android API must never expose a method that accepts an arbitrary balance, verified revenue amount, split result or `PAID` state from the client.

## Wallet calculation
Wallet is derived from immutable ledger states, not from a number stored in the APK.

For verified revenue `R` cents and player basis points `P`:
`player = floor(R * P / 10000)`
`platform = R - player`

Launch default: `P = 7000`, platform = `3000`.

Example: partner confirms EUR 10.00 = 1000 cents. Player receives 700 cents attributable balance and platform receives 300 cents.

## Anti-abuse
- authenticated server user ID
- installation token association and risk history
- verified/unique payout identity
- partner conversion deduplication
- signed postbacks
- rate limiting
- suspicious multi-account review
- payout hold for reversals/chargebacks where partner terms require it
- immutable audit trail

Do not rely solely on Android ID, IP address, local SharedPreferences or email text matching.

## PayPal
PayPal API credentials and webhook secrets are server-only secrets. Never commit them to GitHub and never package them in the APK. Payout is disabled until the PayPal/business setup and legal/compliance requirements are complete.

## Partner activation gate
Real-money mode remains OFF until the chosen advertising/offerwall partner explicitly permits incentivized/cash rewards for this app and provides production credentials/postback documentation. Test/local loyalty points are not money and must remain visually and technically separate from the wallet.

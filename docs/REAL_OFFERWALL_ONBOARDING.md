# REAL OFFERWALL INTEGRATION — NOT A DEMO

## Publisher partners to apply to

1. ayeT Studios: https://www.ayetstudios.com/ — Android SDK v2 / Offerwall API: https://docs.ayetstudios.com/v/product-docs/offerwall/sdk-integrations-v2/android-sdk-v2
2. Torox: https://torox.io/publisher-integration-guide/ — contract + placement + server-side postback.
3. AdGem: https://docs.adgem.com/ — account and app approval required before inventory is available.

## Application details

App: We Earn Together
Platform: Android (native Kotlin)
Package: com.weearntogether.play
Current status: private pre-launch; no real users, no verified conversions
Business model proposed: 50% of verified, non-reversed publisher payout attributable to each player is allocated to that player; 50% to platform, subject to fees, taxes and contract approval.
Payout method planned: PayPal, only after compliance and partner approval.
Country: Germany.
Question for partner: Do your written terms allow converting earned offerwall rewards into real-money PayPal withdrawals for end users? Confirm permissible traffic sources, minimum payout, chargeback windows, fees, GDPR requirements, and whether self-testing of offers is allowed.

## Mandatory before live

- Publisher account and explicit approval of cash rewards
- Placement ID / app key via server secrets, never commit credentials
- Authenticated player accounts with persistent UUID
- HTTPS backend, verified signed postbacks, deduplication by conversion ID
- Pending / approved / reversed ledger; prevent withdrawal on pending rewards
- Fraud checks and provider-specific chargeback handling
- PayPal payouts after verified settlement and KYC/legal compliance
- No fake offers, fabricated earnings, self-referral or incentivized actions outside provider rules

Do not label any existing quiz, memory or math game as a cash-earning game.

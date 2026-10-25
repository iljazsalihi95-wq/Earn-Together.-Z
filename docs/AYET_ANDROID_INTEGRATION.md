# ayeT Android Offerwall integration

- Existing applicationId: com.weearntogether.play (unchanged).
- Android SDK: io.ayet:android-sdk-v2:1.0.3 from Maven Central.
- Placement: 25740; adslot: 30651; SDK adslot name: We Earn Together Offerwall.
- Configured virtual currency: WETCOIN, 1000 units per USD publisher payout, zero decimal places. This is not an EUR cash-out exchange rate.

## Entry points and identity
Home, Profile and Wallet open AyetOfferwallActivity (not exported). On each offerwall/reward-status opening it validates the existing session with the existing Apps Script backend. The SDK receives only the returned immutable user_id after checking ACTIVE status, exact local/server identity match, the unchanged local session and identifier syntax. No email, password or session token is passed to ayeT. SDK initialization is repeated with the freshly validated user ID when another account opens offers.

The official Android SDK is used for the Android placement, not a guessed web integration URL. No Publisher API Key or gateway secret is stored in this repository or APK. No callback URL is used as the offerwall destination.

## Rewards
The Android entry point never calls recordRewardEvent, changes local loyalty/coins or writes a balance when an offerwall closes. Wallet reads the existing authenticated APIs and refreshes its profile when resumed. ayeT offer progress/support is available separately through showRewardStatus.

Real credits still require verified server-to-server callbacks, durable server deduplication and the backend reward rule. At integration time the deployed Apps Script receipt layer records CONFIRMED_UNALLOCATED receipts without crediting coins or cash. This Android commit does not change that layer or claim end-to-end rewards are operational.

## Remaining validation
The placement was in review at setup. ayeT approval, signed callback delivery, sandbox exclusion, real offer availability and a physical-device test with two accounts remain necessary. Do not enable optional callback categories simply to open the SDK. Reversal activation remains pending end-to-end tests. A build passing does not verify partner approval or payment.

Expected device checks: signed-out launch cannot initialize SDK; expired/offline session cannot open; account A and account B each initialize with their own server user_id; returning from offers creates no client credit; wallet refresh reads the backend; review/unavailable service is handled without a local reward or balance claim.

## References
- https://docs.ayetstudios.com/v/product-docs/offerwall/sdk-integrations-v2/android-sdk-v2
- https://github.com/ayetstudios/android-sdk-v2-demo-app

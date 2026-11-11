# Quiz Arena native backend

The Android app keeps the existing account endpoint. When existing `DB` (or alias `ARENA_DB`) is bound,
`/api/arena` authenticates existing Script sessions through `LEGACY_ACCOUNT_URL`
and native Cloudflare sessions through `NativeAccounts`. It runs new rounds in D1
with the same `arenaBootstrap/Register/Start/Resume/Ready/Answer/Next/Leaderboard/Lifeline`
contract. A bank of 232 existing Albanian questions stays server-side; answer keys
and explanations appear only in graded feedback. A round starts at ten questions
and grows to fifteen when its category has enough material. The answer timer gets
sixty seconds after `arenaReady`, which the app sends after narration.

Legacy progress is imported from the existing quiz service when reachable, once
per authenticated player, preserving its exact points/completed levels. An existing
Script round continues through that service until finished. The response field
`legacy_progress_imported` reports whether that import was confirmed; a four-second
optional import timeout does not prevent new D1 gameplay. No unavailable historical
scores are fabricated. A later successful bootstrap retries the import.

The existing account service remains the wallet authority. Bootstrap and completed
round responses include `wallet`, `wallet_backend`, `reward_rules`, and, after a
passed round, `level_award: {coins,status,duplicate}`. NativeAccounts retains its
existing 20 `GAME_ONLY` coins per first completed level. Script accounts use the
existing `recordRewardEvent` rules and return that service's actual amount/status.
D1's wallet audit stores receipts, never another spendable balance. Wallet outages
return `RETRY_PENDING` without losing the completed round; resuming it repairs the
receipt. Rewarded ads use the existing `html5_quiz_arena` SDK receipt flow and remain
separate `PENDING` Reward Coins. Quiz scores and game coins never imply cash.

`/api/ai/speech` and `/api/ai/respond` authenticate the same owned D1 or legacy round.
The existing server OpenAI key and durable quotas remain in use. R2 `BUCKET` (or alias `ARENA_MEDIA`)
caches private MP3s and dialogue JSON under content hashes with no public object
route. Audio hits still require authentication and the correct question/phase.
Presenter and telephone friend use distinct voices (`ARENA_HOST_VOICE`, default
`onyx`; `ARENA_FRIEND_VOICE`, default `echo`). No client voice name/key is trusted.

## Deployment

The existing GitHub Actions Worker deployment validates the code, then runs
`scripts/provision-storage.mjs` using the existing Cloudflare repository secrets.
It resolves the existing D1 `lazy-ai-studio` and R2 `site-creator-r2` by exact name,
never creates replacements, applies the repeatable arena-only SQL schema, and writes `.arena-wrangler.toml` with
the returned database ID and existing bindings `DB` and `BUCKET`. MP3/dialogue objects
use a `quiz-arena/` namespace so existing AI Studio assets remain separate. Wrangler deploys that generated configuration with the
existing Worker variables/secrets preserved. The Cloudflare token needs D1 and R2
write permissions in addition to Workers deployment permissions. No credentials,
database contents, or token fragments are printed. Storage calls have a twenty
second timeout and deployment stops if provisioning fails.

For a local authorized deployment, run the provisioning script from the repository
root with the existing Cloudflare credential environment, then deploy the generated
configuration. Do not commit `.arena-wrangler.toml` or replace IDs with placeholders.
The health endpoint exposes `arena_d1_configured`, `audio_r2_configured`,
`legacy_accounts_configured`, and `speech_revision: 11`. A passing local test proves
logic under fixtures; deployment, authenticated live playback and phone-device
verification need their own evidence.

## Tests

`node --test quiz-arena/cloudflare-worker/test/*.test.js`

The D1 tests run the real migration and SQL against Node's SQLite engine, verifying
server grading, replay safety, a sixty-second post-reading clock, ownership, legacy
progress import, wallet separation/idempotency, R2 reuse after cold edge caches,
and provisioning error redaction. Paid APIs are replaced with deterministic fixtures.

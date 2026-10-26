# HTML5 Play Studio — 13 games

Home, Games, Quests and the six previous native game entry functions now open bundled HTML5 games. Original native sources and saves remain intact. GamePix and ayeT keep their existing separate entries.

| Game | Mechanics | Stage goal |
|---|---|---|
| Word Trails | Letter tracing / tap and submit | Find authored words |
| Memory Bloom | Preview, pair matching, limited turns | Match every pair |
| Garden Restore | Adjacent swaps, cascades, estate chapter art | Reach stage score in limited moves |
| Bubble Voyage | Aiming, wall bounces, hex adjacency, detached clusters | Clear all bubbles |
| 2048 Fusion | Swipes, single-merge rule, seeded spawns | Reach 128 through 2048 as stages rise |
| Falling Blocks | Seven tetrominoes, rotation, wall kicks, hard drop | Clear the required rows |
| Block Atelier | Three-shape tray, 8×8 placement, rows and columns | Reach stage score |
| Arrow Escape | Bent path arrows, blocked rays, hearts | Remove every path |
| Treasure Wheel | Rotating jewel wheel, timed shots, blade collisions | Collect every jewel |
| Water Sort | Same-color / empty tube pours | Finish every color tube |
| Parking Escape | Axis-constrained car slides and collisions | Move the gold car to the exit |
| Color Lines | Reachable marble moves, five-in-line, seeded additions | Reach stage score |
| Neon Stack | Moving platform, overlap cutting, perfect drops | Build the required floors |

Every game exposes 40 level challenges: 520 total. Progress, stars and gameplay tokens are device-local and scoped to the signed-in account. This does not claim cloud migration of old native saves. Word stages 21–40 reuse vocabulary with more targets and tighter attempts. These are original bundled implementations; no third-party artwork, proprietary code or licensed premium game is copied. Garden Restore includes match-3 objectives and estate chapters, not the separate LazyGarden project's full character/story renovation system.

## Ads and coins

Every game has the same optional rewarded-video boost and a natural-break interstitial attempt on Next challenge. No inventory continues immediately; no completion means no boost or coin receipt. Only `LevelPlay.onAdRewarded` calls `Html5VideoRewards.completed`; JavaScript cannot directly create receipts or set coin values. Rewarded completion is durable even if the activity closes before the ad-close UI callback. Game-only hints still cost 25 gameplay tokens, earned at 15 per first level completion.

Completed videos are queued per account in private native storage, with native UUIDs and a 1,000-waiting-receipt limit. Synced entries can be trimmed locally; the server retains authoritative history. Account changes block cross-account sync. Offline/error/older-server responses keep the video waiting. Native summary labels waiting videos separately from actual server-recorded pending coins. The wallet button opens the existing authenticated wallet. The hub uses cached server pending/verified coin balances when available, plus a separate count of waiting video receipts; it refreshes the server balance at most once per minute. The wallet remains the full authoritative account view.

The existing `Wallet.gs` is extended, not replaced with a new backend/database. `startRewardSession` advertises `ad_reward_coins` for `html5_` games. The owner can set Script Property `WALLET_HTML5_VIDEO_COINS` to an integer 0–1000; default 10. This is an initial **noncash PENDING coin rule**, not a coin-to-money exchange rate. Replayed events return their originally recorded coin amount even if the rule changes. Existing session ownership, expiry, interval, rate, audit and client-authority checks still apply. Native SDK reports remain client evidence; independent verification is still necessary before coins become VERIFIED. No revenue or cash is minted and no automatic payouts are enabled.

### Deployment boundary

The GitHub commit contains the backend rule and Android queue. It does not itself publish a new Google Apps Script web-app version. Update the existing Apps Script project's Wallet.gs and deploy a new version of its existing deployment before expecting videos to become wallet pending coins. Preserve the same deployment URL, auth files, properties and spreadsheet. Older live deployments do not advertise the rule; Android retains receipts as waiting videos rather than reporting invented coins. A completed rewarded video in a physical device and its server receipt still require live acceptance testing.

## Verification

`node --test tests/html5-games.test.cjs tests/html5-arcade.test.cjs` executes 332 mechanics checks. Every one of 40 Water Sort / Parking / Arrow levels is checked against a removal or reversal solution; Garden starts without matches and with valid moves; engine tests cover merge-once, line clears, collisions, target completion and revival boundaries.

`node tests/html5-browser.cjs` (Playwright path optionally supplied by PLAYWRIGHT_MODULE) exercises all 13 actual interfaces, selected inputs, word/memory/arrow completion, unlock persistence, responsive widths, no-fill/cancelled video and simulated SDK boost completion. It captures screenshots as a CI artifact. Browser bridge mocks do not confirm real mediation inventory, physical-device touch, S2S verification or money.

`node backend/tests/wallet.test.cjs` verifies wallet authority, pending video rules, configurable values and idempotent amounts in isolated in-memory Script services. CI also builds the Android APK. Real ad reward ordering and account/logout/resume cases require physical-device verification.

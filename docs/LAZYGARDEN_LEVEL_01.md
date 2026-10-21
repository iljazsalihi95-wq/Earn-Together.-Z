# LazyGarden — Level 01

Implementation target: original premium match-3 game integrated into We Earn Together.

## Level 01
- Board: 8x8
- Moves: 24
- Goal: collect 20 leaves and 20 flowers
- Valid adjacent swaps only
- Match 3+ detection, cascades and refill
- 4-match creates a directional rocket
- 5-match creates a garden prism
- Persistent in-game coins; coins never expire
- Win reward: 50 LazyGarden Coins
- Save/resume current level state

## Visual direction
Mature premium garden aesthetic with original LazyGarden artwork. Layered garden background, stone/glass board treatment, polished botanical tiles, depth, particles and restrained animation. No Playrix/Gardenscapes artwork, characters, sounds, level data or proprietary assets are copied.

## Localization
UI strings must be resource-based. Initial target locales: English, German, Italian, Spanish, French, Portuguese and Turkish; device locale selects the language.

## Reward boundary
LazyGarden Coins are game currency. Any future cash-convertible reward must be accounted separately and only after a rewarded-ad/provider event is verifiably eligible.
# Bundled HTML5 Play Studio

Word Trails and Memory Bloom ship inside the APK and each expose 40 challenges. Main Home, Games and Quests open the HTML5 host. GamePix and ayeT entries remain available. Legacy native game code and saves remain intact.

Word Trails uses authored word lists, letter tracing, attempts, stars and hints. Memory Bloom uses preview, pair matching, turn limits and increasing board size. Levels 21–40 of Word Trails reuse the vocabulary with more targets and fewer attempts. These are original bundled games, not licensed third-party premium titles.

Progress and gameplay tokens are stored locally per signed-in account. They are not cloud synced and do not create wallet coins, cash or loyalty rewards. First completion awards 15 gameplay tokens; a hint costs 25. Existing account and wallet flows retain their server authority.

The host allows only four bundled assets on a virtual HTTPS origin. No external page can navigate with the JavaScript bridge. State is bounded and validated before persistence. Account changes invalidate the session.

Optional rewarded boosts add five attempts/turns only after the LevelPlay reward callback and ad closure. Missing inventory or cancelled video grants nothing. Interstitials are attempted after completed levels at the Next action, with the existing minimum interval; no fill continues immediately. Ads use the app's configured LevelPlay units. Device testing with a configured mediation account is still required to confirm real inventory and callback ordering.

Run `node --test tests/html5-games.test.cjs` for all 80 levels and engine edge cases. CI also compiles the Android APK. Visual and real-device acceptance should cover small portrait and landscape screens, touch tracing, rotation, app backgrounding, offline launch, account separation and ad completion/cancellation. Source tests do not confirm live ad revenue or provider approval.

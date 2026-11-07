package com.weearntogether.play

import android.content.Context

/**
 * Device-only, non-cash gameplay currency for the private Android pilot.
 * Never use this balance as RewardCoins, cash, or proof of a paid ad conversion.
 * Each signed-in account has its own local balance; server-backed purchases
 * and redeemable rewards require a separate verified ledger.
 */
class GameCoinStore(context: Context) {
    private val app = context.applicationContext
    private val account = AccountStore(app)
    companion object {
        const val LEVEL_COINS = 20L
        const val VIDEO_COINS = 100L
        // Activities and ad callbacks construct separate stores for the same file.
        private val lock = Any()
    }
    private val prefs = app.getSharedPreferences("wet_gamecoins_pilot_v1", Context.MODE_PRIVATE)

    fun balance(): Long = synchronized(lock) {
        val user = account.currentUserId() ?: return@synchronized 0L
        prefs.getLong("balance:$user", 0L).coerceAtLeast(0L)
    }

    /** Called only by the native rewarded-ad SDK completion callback. */
    fun awardCompletedVideo(): Boolean = synchronized(lock) {
        val user = account.currentUserId() ?: return@synchronized false
        val key = "balance:$user"
        val current = prefs.getLong(key, 0L).coerceAtLeast(0L)
        if (current > Long.MAX_VALUE - VIDEO_COINS) return@synchronized false
        prefs.edit().putLong(key, current + VIDEO_COINS).commit()
    }

    /** Award once per account, game and completed level, including after restart. */
    fun awardCompletedLevel(gameId: String, level: Int): Boolean = synchronized(lock) {
        val user = account.currentUserId() ?: return@synchronized false
        if (gameId.isBlank() || level < 1) return@synchronized false
        val receipt = "level:$user:$gameId:$level"
        if (prefs.getBoolean(receipt, false)) return@synchronized false
        val key = "balance:$user"
        val current = prefs.getLong(key, 0L).coerceAtLeast(0L)
        if (current > Long.MAX_VALUE - LEVEL_COINS) return@synchronized false
        prefs.edit().putBoolean(receipt, true).putLong(key, current + LEVEL_COINS).commit()
    }

    fun spend(cost: Long): Boolean = synchronized(lock) {
        val user = account.currentUserId() ?: return@synchronized false
        if (cost <= 0L) return@synchronized false
        val key = "balance:$user"
        val current = prefs.getLong(key, 0L).coerceAtLeast(0L)
        if (current < cost) return@synchronized false
        prefs.edit().putLong(key, current - cost).commit()
    }
}

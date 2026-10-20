package com.weearntogether.play

import android.content.Context

/**
 * Local, non-monetary loyalty progress.
 * This deliberately does NOT represent cash, ad revenue, or a withdrawable balance.
 * Monetary rewards can only be added after an approved provider/backend exists.
 */
class LoyaltyProgress(context: Context) {
    private val prefs = context.getSharedPreferences("wet_loyalty_progress", Context.MODE_PRIVATE)

    val points: Int get() = prefs.getInt("points", 0)
    val gamesPlayed: Int get() = prefs.getInt("games_played", 0)
    val bestStreak: Int get() = prefs.getInt("best_streak", 0)
    val currentStreak: Int get() = prefs.getInt("current_streak", 0)
    val lastPlayedAt: Long get() = prefs.getLong("last_played_at", 0L)

    fun recordGameProgress(pointsEarned: Int) {
        if (pointsEarned <= 0) return
        val now = System.currentTimeMillis()
        val last = lastPlayedAt
        val day = 86_400_000L
        val newStreak = when {
            last <= 0L -> 1
            now - last < day -> currentStreak.coerceAtLeast(1)
            now - last < day * 2 -> currentStreak + 1
            else -> 1
        }
        prefs.edit()
            .putInt("points", (points + pointsEarned).coerceAtMost(1_000_000_000))
            .putInt("games_played", gamesPlayed + 1)
            .putInt("current_streak", newStreak)
            .putInt("best_streak", maxOf(bestStreak, newStreak))
            .putLong("last_played_at", now)
            .apply()
    }
}

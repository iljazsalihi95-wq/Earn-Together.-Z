package com.weearntogether.play

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local non-monetary loyalty progress. Never represents cash or withdrawable balance. */
class LoyaltyProgress(context: Context) {
    private val prefs = context.getSharedPreferences("wet_loyalty_progress", Context.MODE_PRIVATE)
    val points: Int get() = prefs.getInt("points", 0)
    val gamesPlayed: Int get() = prefs.getInt("games_played", 0)
    val bestStreak: Int get() = prefs.getInt("best_streak", 0)
    val currentStreak: Int get() = prefs.getInt("current_streak", 0)
    val lastPlayedAt: Long get() = prefs.getLong("last_played_at", 0L)
    val todayGames: Int get() { resetDailyIfNeeded(); return prefs.getInt("today_games", 0) }
    val dailyGoal: Int get() = 3
    val dailyClaimed: Boolean get() { resetDailyIfNeeded(); return prefs.getBoolean("daily_claimed", false) }

    private fun dayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun resetDailyIfNeeded() {
        val today = dayKey()
        if (prefs.getString("daily_day", "") != today) prefs.edit().putString("daily_day", today).putInt("today_games", 0).putBoolean("daily_claimed", false).apply()
    }

    fun recordGameProgress(pointsEarned: Int) {
        if (pointsEarned <= 0) return
        resetDailyIfNeeded()
        val now = System.currentTimeMillis(); val last = lastPlayedAt; val day = 86_400_000L
        val newStreak = when { last <= 0L -> 1; now - last < day -> currentStreak.coerceAtLeast(1); now - last < day * 2 -> currentStreak + 1; else -> 1 }
        prefs.edit().putInt("points", (points + pointsEarned).coerceAtMost(1_000_000_000))
            .putInt("games_played", gamesPlayed + 1).putInt("today_games", prefs.getInt("today_games", 0) + 1)
            .putInt("current_streak", newStreak).putInt("best_streak", maxOf(bestStreak, newStreak)).putLong("last_played_at", now).apply()
    }

    /** Claims 100 local loyalty points once per day after 3 recorded game sessions. No cash value. */
    fun claimDailyChallenge(): Boolean {
        resetDailyIfNeeded()
        if (dailyClaimed || todayGames < dailyGoal) return false
        prefs.edit().putInt("points", (points + 100).coerceAtMost(1_000_000_000)).putBoolean("daily_claimed", true).apply()
        return true
    }
}

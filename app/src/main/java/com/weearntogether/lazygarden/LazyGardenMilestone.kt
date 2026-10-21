package com.weearntogether.lazygarden

import android.content.Context

/**
 * Tracks the first reward milestone without creating a real-money balance.
 * The €1 value is presentation-only until the ad/reward model is verified.
 */
class LazyGardenMilestone(context: Context) {
    private val prefs = context.getSharedPreferences("lazy_garden_milestone", Context.MODE_PRIVATE)

    val completedLevels: Int
        get() = prefs.getInt(KEY_COMPLETED_LEVELS, 0)

    val targetLevels: Int = 10
    val demoEuroValue: Double = 1.0

    fun markLevelCompleted(level: Int): Int {
        val key = "level_${level}_counted"
        if (prefs.getBoolean(key, false)) return completedLevels
        val next = (completedLevels + 1).coerceAtMost(targetLevels)
        prefs.edit()
            .putBoolean(key, true)
            .putInt(KEY_COMPLETED_LEVELS, next)
            .apply()
        return next
    }

    fun isDemoMilestoneReached(): Boolean = completedLevels >= targetLevels

    companion object {
        private const val KEY_COMPLETED_LEVELS = "completed_levels"
    }
}

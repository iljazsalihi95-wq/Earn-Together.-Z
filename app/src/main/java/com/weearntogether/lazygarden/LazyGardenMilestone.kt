package com.weearntogether.lazygarden

import android.content.Context
import com.weearntogether.play.ProgressPreferences

/**
 * Tracks campaign milestones only. This class deliberately contains no cash value,
 * payout promise, or real-money conversion: LazyGarden progression is gameplay-only
 * until an approved reward partner is integrated.
 */
class LazyGardenMilestone(context: Context) {
    private val prefs = ProgressPreferences.open(context, "lazy_garden_milestone")

    val completedLevels: Int
        get() = prefs.getInt(KEY_COMPLETED_LEVELS, 0).coerceIn(0, CAMPAIGN_LEVELS)

    val targetLevels: Int = CAMPAIGN_LEVELS

    /**
     * Campaign progression is sequential. A replay is harmless, while an out-of-order
     * completion cannot skip restoration chapters or inflate campaign progress.
     */
    fun markLevelCompleted(level: Int): Int {
        val safeLevel = level.coerceIn(1, CAMPAIGN_LEVELS)
        val current = completedLevels
        if (safeLevel <= current) return current
        if (safeLevel != current + 1) return current

        prefs.edit()
            .putBoolean("level_${safeLevel}_counted", true)
            .putInt(KEY_COMPLETED_LEVELS, safeLevel)
            .apply()
        return safeLevel
    }

    /** First campaign level that still needs to be completed. */
    val nextRequiredLevel: Int
        get() = (completedLevels + 1).coerceAtMost(CAMPAIGN_LEVELS)

    fun isLevelUnlocked(level: Int): Boolean {
        val safeLevel = level.coerceIn(1, CAMPAIGN_LEVELS)
        return safeLevel <= nextRequiredLevel
    }

    /** Ten-level chapters match the 25 restoration areas in the campaign. */
    val completedChapters: Int
        get() = (completedLevels / LEVELS_PER_CHAPTER).coerceAtMost(CHAPTERS)

    val currentChapter: Int
        get() = (completedLevels / LEVELS_PER_CHAPTER + 1).coerceAtMost(CHAPTERS)

    val levelsIntoCurrentChapter: Int
        get() = if (completedLevels >= CAMPAIGN_LEVELS) LEVELS_PER_CHAPTER
        else completedLevels % LEVELS_PER_CHAPTER

    fun isChapterComplete(chapter: Int): Boolean {
        val safeChapter = chapter.coerceIn(1, CHAPTERS)
        return completedLevels >= safeChapter * LEVELS_PER_CHAPTER
    }

    fun isCampaignComplete(): Boolean = completedLevels >= CAMPAIGN_LEVELS

    companion object {
        private const val KEY_COMPLETED_LEVELS = "completed_levels"
        private const val CAMPAIGN_LEVELS = 250
        private const val LEVELS_PER_CHAPTER = 10
        private const val CHAPTERS = CAMPAIGN_LEVELS / LEVELS_PER_CHAPTER
    }
}

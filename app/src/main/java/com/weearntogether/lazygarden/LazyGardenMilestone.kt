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

    /**
     * Migrates players who already restored sequential levels before milestone tracking
     * was wired into the live activity. The caller must supply a contiguous restoration
     * count derived from saved build tasks; arbitrary level jumps are never accepted.
     */
    fun syncCompletedThrough(contiguousRestoredLevels: Int): Int {
        val target = contiguousRestoredLevels.coerceIn(0, CAMPAIGN_LEVELS)
        var current = completedLevels
        if (target <= current) return current
        val editor = prefs.edit()
        for (level in (current + 1)..target) editor.putBoolean("level_${level}_counted", true)
        editor.putInt(KEY_COMPLETED_LEVELS, target).apply()
        current = target
        return current
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

    /** Stable 0..100 progress value for restoration UI and chapter presentation. */
    val currentChapterProgressPercent: Int
        get() = if (isCampaignComplete()) 100
        else (levelsIntoCurrentChapter * 100 / LEVELS_PER_CHAPTER).coerceIn(0, 100)

    /** Stable 0..100 progress value for the complete 250-level campaign. */
    val campaignProgressPercent: Int
        get() = (completedLevels * 100 / CAMPAIGN_LEVELS).coerceIn(0, 100)

    /** A chapter becomes available only after every preceding chapter is finished. */
    fun isChapterUnlocked(chapter: Int): Boolean {
        val safeChapter = chapter.coerceIn(1, CHAPTERS)
        return safeChapter <= currentChapter
    }

    fun isChapterComplete(chapter: Int): Boolean {
        val safeChapter = chapter.coerceIn(1, CHAPTERS)
        return completedLevels >= safeChapter * LEVELS_PER_CHAPTER
    }

    /**
     * Gives presentation code an exact level range without duplicating campaign math.
     * This keeps restoration scenes aligned with their ten playable levels.
     */
    fun chapterLevelRange(chapter: Int): IntRange {
        val safeChapter = chapter.coerceIn(1, CHAPTERS)
        val first = (safeChapter - 1) * LEVELS_PER_CHAPTER + 1
        return first..(first + LEVELS_PER_CHAPTER - 1)
    }

    fun isCampaignComplete(): Boolean = completedLevels >= CAMPAIGN_LEVELS

    companion object {
        private const val KEY_COMPLETED_LEVELS = "completed_levels"
        private const val CAMPAIGN_LEVELS = 250
        private const val LEVELS_PER_CHAPTER = 10
        private const val CHAPTERS = CAMPAIGN_LEVELS / LEVELS_PER_CHAPTER
    }
}

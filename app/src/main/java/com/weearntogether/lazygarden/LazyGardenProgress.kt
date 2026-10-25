package com.weearntogether.lazygarden

import android.content.Context
import com.weearntogether.play.ProgressPreferences

data class LazyGardenProgress(
    val level: Int,
    val unlockedLevel: Int,
    val movesLeft: Int,
    val collected: Map<GardenTile, Int>,
    val clearedObstacles: Map<GardenObstacle, Int> = emptyMap(),
    val boardState: String = "",
    val obstacleState: String = "",
    val shuffleBoosters: Int = 3,
    val obstacleHitState: String = ""
)

class LazyGardenProgressStore(context: Context) {
    private val prefs = ProgressPreferences.open(context,"lazy_garden_progress")

    fun save(progress: LazyGardenProgress) {
        val safeLevel = progress.level.coerceIn(1, LazyGardenLevels.count)
        val safeUnlocked = progress.unlockedLevel.coerceIn(safeLevel, LazyGardenLevels.count)
        val maxMoves = LazyGardenLevels.get(safeLevel).moves
        val safeBoard = progress.boardState.takeIf { isValidBoardState(it, safeLevel) }.orEmpty()
        val safeObstacles = progress.obstacleState.takeIf { isValidObstacleState(it, safeLevel) }.orEmpty()
        val safeObstacleHits = progress.obstacleHitState.takeIf { isValidObstacleHitState(it, safeLevel) }.orEmpty()
        val hasCompleteSession = safeBoard.isNotEmpty() && safeObstacles.isNotEmpty()
        val editor = prefs.edit()
            .putInt("save_version", SAVE_VERSION)
            .putInt("level", safeLevel)
            .putInt("session_level", safeLevel)
            .putInt("unlocked_level", safeUnlocked)
            .putInt("moves", if (hasCompleteSession) progress.movesLeft.coerceIn(0, maxMoves) else maxMoves)
            .putString("board_state", if (hasCompleteSession) safeBoard else "")
            .putString("obstacle_state", if (hasCompleteSession) safeObstacles else "")
            .putString("obstacle_hit_state", if (hasCompleteSession) safeObstacleHits else "")
            .putInt("shuffle_boosters", progress.shuffleBoosters.coerceIn(0, MAX_SHUFFLE_BOOSTERS))
        GardenTile.entries.forEach { tile -> editor.putInt("tile_${tile.name}", if (hasCompleteSession) (progress.collected[tile] ?: 0).coerceAtLeast(0) else 0) }
        GardenObstacle.entries.forEach { obstacle -> editor.putInt("obstacle_${obstacle.name}", if (hasCompleteSession) (progress.clearedObstacles[obstacle] ?: 0).coerceAtLeast(0) else 0) }
        editor.apply()
    }

    fun load(): LazyGardenProgress {
        val level = prefs.getInt("level", 1).coerceIn(1, LazyGardenLevels.count)
        val unlocked = prefs.getInt("unlocked_level", level).coerceIn(level, LazyGardenLevels.count)
        val maxMoves = LazyGardenLevels.get(level).moves
        val sameSession = prefs.getInt("session_level", level) == level
        val storedBoard = if (sameSession) prefs.getString("board_state", "") ?: "" else ""
        val storedObstacles = if (sameSession) prefs.getString("obstacle_state", "") ?: "" else ""
        val storedObstacleHits = if (sameSession) prefs.getString("obstacle_hit_state", "") ?: "" else ""
        val boardState = storedBoard.takeIf { isValidBoardState(it, level) }.orEmpty()
        val obstacleState = storedObstacles.takeIf { isValidObstacleState(it, level) }.orEmpty()
        val obstacleHitState = storedObstacleHits.takeIf { isValidObstacleHitState(it, level) }.orEmpty()
        val resumable = sameSession && boardState.isNotEmpty() && obstacleState.isNotEmpty()
        return LazyGardenProgress(
            level = level,
            unlockedLevel = unlocked,
            movesLeft = if (resumable) prefs.getInt("moves", maxMoves).coerceIn(0, maxMoves) else maxMoves,
            collected = GardenTile.entries.associateWith { tile ->
                if (!resumable) 0 else prefs.getInt("tile_${tile.name}", if (tile == GardenTile.LEAF) prefs.getInt("leaf", 0) else if (tile == GardenTile.FLOWER) prefs.getInt("flower", 0) else 0).coerceAtLeast(0)
            },
            clearedObstacles = GardenObstacle.entries.associateWith { obstacle -> if (resumable) prefs.getInt("obstacle_${obstacle.name}", 0).coerceAtLeast(0) else 0 },
            boardState = if (resumable) boardState else "",
            obstacleState = if (resumable) obstacleState else "",
            shuffleBoosters = prefs.getInt("shuffle_boosters", 3).coerceIn(0, MAX_SHUFFLE_BOOSTERS),
            obstacleHitState = if (resumable) obstacleHitState else ""
        )
    }

    fun clearSessionBoard() {
        val editor = prefs.edit().remove("board_state").remove("obstacle_state").remove("obstacle_hit_state").remove("session_level").remove("moves")
        GardenTile.entries.forEach { editor.remove("tile_${it.name}") }
        GardenObstacle.entries.forEach { editor.remove("obstacle_${it.name}") }
        editor.apply()
    }

    fun markRewardClaimed(level: Int): Boolean {
        if (level !in 1..LazyGardenLevels.count) return false
        val key = "reward_level_$level"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    /** Keeps the best 1-3 star result for each campaign level. Replays can improve it, never reduce it. */
    fun recordBestStars(level: Int, stars: Int): Int {
        if (level !in 1..LazyGardenLevels.count) return 0
        val safeStars = stars.coerceIn(1, 3)
        val key = "stars_level_$level"
        val previous = prefs.getInt(key, 0).coerceIn(0, 3)
        if (safeStars > previous) prefs.edit().putInt(key, safeStars).apply()
        return maxOf(previous, safeStars)
    }

    fun bestStars(level: Int): Int = if (level in 1..LazyGardenLevels.count) prefs.getInt("stars_level_$level", 0).coerceIn(0, 3) else 0

    fun totalStars(): Int = (1..LazyGardenLevels.count).sumOf { bestStars(it) }

    fun markBuildTaskCompleted(level: Int) {
        if (level in 1..LazyGardenLevels.count) prefs.edit().putBoolean("build_task_$level", true).apply()
    }
    fun isBuildTaskCompleted(level: Int): Boolean = level in 1..LazyGardenLevels.count && prefs.getBoolean("build_task_$level", false)

    private fun isValidBoardState(state: String, level: Int): Boolean {
        val spec = LazyGardenLevels.get(level)
        val cells = spec.rows * spec.columns
        val maxTile = GardenTile.entries.lastIndex.digitToChar()
        return state.length == cells && state.all { it in '0'..maxTile }
    }

    private fun isValidObstacleState(state: String, level: Int): Boolean {
        val spec = LazyGardenLevels.get(level)
        val cells = spec.rows * spec.columns
        return state.length == cells && state.all { it in '0'..'3' }
    }

    private fun isValidObstacleHitState(state: String, level: Int): Boolean {
        if (state.isEmpty()) return false
        val spec = LazyGardenLevels.get(level)
        val rows = state.split(',')
        if (rows.size != spec.rows) return false
        return rows.all { row ->
            val values = row.split('.')
            values.size == spec.columns && values.all { value -> value.toIntOrNull()?.let { it in 0..3 } == true }
        }
    }

    companion object {
        private const val SAVE_VERSION = 5
        private const val MAX_SHUFFLE_BOOSTERS = 99
    }
}

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
    val shuffleBoosters: Int = 3
)

class LazyGardenProgressStore(context: Context) {
    private val prefs = ProgressPreferences.open(context,"lazy_garden_progress")

    fun save(progress: LazyGardenProgress) {
        val safeLevel = progress.level.coerceIn(1, LazyGardenLevels.count)
        val safeUnlocked = progress.unlockedLevel.coerceIn(safeLevel, LazyGardenLevels.count)
        val maxMoves = LazyGardenLevels.get(safeLevel).moves
        val safeBoard = progress.boardState.takeIf { isValidBoardState(it, safeLevel) }.orEmpty()
        val safeObstacles = progress.obstacleState.takeIf { isValidObstacleState(it, safeLevel) }.orEmpty()
        val editor = prefs.edit()
            .putInt("save_version", SAVE_VERSION)
            .putInt("level", safeLevel)
            .putInt("session_level", safeLevel)
            .putInt("unlocked_level", safeUnlocked)
            .putInt("moves", progress.movesLeft.coerceIn(0, maxMoves))
            .putString("board_state", safeBoard)
            .putString("obstacle_state", safeObstacles)
            .putInt("shuffle_boosters", progress.shuffleBoosters.coerceIn(0, MAX_SHUFFLE_BOOSTERS))
        GardenTile.entries.forEach { tile -> editor.putInt("tile_${tile.name}", (progress.collected[tile] ?: 0).coerceAtLeast(0)) }
        GardenObstacle.entries.forEach { obstacle -> editor.putInt("obstacle_${obstacle.name}", (progress.clearedObstacles[obstacle] ?: 0).coerceAtLeast(0)) }
        editor.apply()
    }

    fun load(): LazyGardenProgress {
        val level = prefs.getInt("level", 1).coerceIn(1, LazyGardenLevels.count)
        val unlocked = prefs.getInt("unlocked_level", level).coerceIn(level, LazyGardenLevels.count)
        val maxMoves = LazyGardenLevels.get(level).moves
        val sameSession = prefs.getInt("session_level", level) == level
        val storedBoard = if (sameSession) prefs.getString("board_state", "") ?: "" else ""
        val storedObstacles = if (sameSession) prefs.getString("obstacle_state", "") ?: "" else ""
        val boardState = storedBoard.takeIf { isValidBoardState(it, level) }.orEmpty()
        val obstacleState = storedObstacles.takeIf { isValidObstacleState(it, level) }.orEmpty()
        val resumable = sameSession && boardState.isNotEmpty()
        return LazyGardenProgress(
            level = level,
            unlockedLevel = unlocked,
            movesLeft = if (resumable) prefs.getInt("moves", maxMoves).coerceIn(0, maxMoves) else maxMoves,
            collected = GardenTile.entries.associateWith { tile ->
                if (!resumable) 0 else prefs.getInt("tile_${tile.name}", if (tile == GardenTile.LEAF) prefs.getInt("leaf", 0) else if (tile == GardenTile.FLOWER) prefs.getInt("flower", 0) else 0).coerceAtLeast(0)
            },
            clearedObstacles = GardenObstacle.entries.associateWith { obstacle -> if (resumable) prefs.getInt("obstacle_${obstacle.name}", 0).coerceAtLeast(0) else 0 },
            boardState = boardState,
            obstacleState = if (resumable) obstacleState else "",
            shuffleBoosters = prefs.getInt("shuffle_boosters", 3).coerceIn(0, MAX_SHUFFLE_BOOSTERS)
        )
    }

    fun clearSessionBoard() {
        val editor = prefs.edit().remove("board_state").remove("obstacle_state").remove("session_level").remove("moves")
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
        if (state.isEmpty()) return false
        val spec = LazyGardenLevels.get(level)
        val cells = spec.rows * spec.columns
        return state.length in cells..(cells * 8)
    }

    private fun isValidObstacleState(state: String, level: Int): Boolean {
        if (state.isEmpty()) return true
        val spec = LazyGardenLevels.get(level)
        val cells = spec.rows * spec.columns
        return state.length <= cells * 8
    }

    companion object {
        private const val SAVE_VERSION = 3
        private const val MAX_SHUFFLE_BOOSTERS = 99
    }
}

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
        val editor = prefs.edit()
            .putInt("level", safeLevel)
            .putInt("unlocked_level", safeUnlocked)
            .putInt("moves", progress.movesLeft.coerceIn(0, maxMoves))
            .putString("board_state", progress.boardState)
            .putString("obstacle_state", progress.obstacleState)
            .putInt("shuffle_boosters", progress.shuffleBoosters.coerceIn(0, MAX_SHUFFLE_BOOSTERS))
        GardenTile.entries.forEach { tile -> editor.putInt("tile_${tile.name}", (progress.collected[tile] ?: 0).coerceAtLeast(0)) }
        GardenObstacle.entries.forEach { obstacle -> editor.putInt("obstacle_${obstacle.name}", (progress.clearedObstacles[obstacle] ?: 0).coerceAtLeast(0)) }
        editor.apply()
    }

    fun load(): LazyGardenProgress {
        val level = prefs.getInt("level", 1).coerceIn(1, LazyGardenLevels.count)
        val unlocked = prefs.getInt("unlocked_level", level).coerceIn(level, LazyGardenLevels.count)
        val maxMoves = LazyGardenLevels.get(level).moves
        return LazyGardenProgress(
            level = level,
            unlockedLevel = unlocked,
            movesLeft = prefs.getInt("moves", maxMoves).coerceIn(0, maxMoves),
            collected = GardenTile.entries.associateWith { tile ->
                prefs.getInt("tile_${tile.name}", if (tile == GardenTile.LEAF) prefs.getInt("leaf", 0) else if (tile == GardenTile.FLOWER) prefs.getInt("flower", 0) else 0).coerceAtLeast(0)
            },
            clearedObstacles = GardenObstacle.entries.associateWith { obstacle -> prefs.getInt("obstacle_${obstacle.name}", 0).coerceAtLeast(0) },
            boardState = prefs.getString("board_state", "") ?: "",
            obstacleState = prefs.getString("obstacle_state", "") ?: "",
            shuffleBoosters = prefs.getInt("shuffle_boosters", 3).coerceIn(0, MAX_SHUFFLE_BOOSTERS)
        )
    }

    fun clearSessionBoard() { prefs.edit().remove("board_state").remove("obstacle_state").apply() }

    fun markRewardClaimed(level: Int): Boolean {
        if (level !in 1..LazyGardenLevels.count) return false
        val key = "reward_level_$level"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    fun markBuildTaskCompleted(level: Int) {
        if (level in 1..LazyGardenLevels.count) prefs.edit().putBoolean("build_task_$level", true).apply()
    }
    fun isBuildTaskCompleted(level: Int): Boolean = level in 1..LazyGardenLevels.count && prefs.getBoolean("build_task_$level", false)

    companion object { private const val MAX_SHUFFLE_BOOSTERS = 99 }
}

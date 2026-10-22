package com.weearntogether.lazygarden

import android.content.Context

data class LazyGardenProgress(
    val level: Int,
    val unlockedLevel: Int,
    val movesLeft: Int,
    val collected: Map<GardenTile, Int>,
    val clearedObstacles: Map<GardenObstacle, Int> = emptyMap(),
    val boardState: String = "",
    val obstacleState: String = ""
)

class LazyGardenProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("lazy_garden_progress", Context.MODE_PRIVATE)

    fun save(progress: LazyGardenProgress) {
        val editor = prefs.edit()
            .putInt("level", progress.level)
            .putInt("unlocked_level", progress.unlockedLevel)
            .putInt("moves", progress.movesLeft)
            .putString("board_state", progress.boardState)
            .putString("obstacle_state", progress.obstacleState)
        GardenTile.entries.forEach { tile -> editor.putInt("tile_${tile.name}", progress.collected[tile] ?: 0) }
        GardenObstacle.entries.forEach { obstacle -> editor.putInt("obstacle_${obstacle.name}", progress.clearedObstacles[obstacle] ?: 0) }
        editor.apply()
    }

    fun load(): LazyGardenProgress {
        val level = prefs.getInt("level", 1).coerceIn(1, LazyGardenLevels.count)
        return LazyGardenProgress(
            level = level,
            unlockedLevel = prefs.getInt("unlocked_level", level).coerceIn(1, LazyGardenLevels.count),
            movesLeft = prefs.getInt("moves", LazyGardenLevels.get(level).moves),
            collected = GardenTile.entries.associateWith { tile ->
                prefs.getInt("tile_${tile.name}", if (tile == GardenTile.LEAF) prefs.getInt("leaf", 0) else if (tile == GardenTile.FLOWER) prefs.getInt("flower", 0) else 0)
            },
            clearedObstacles = GardenObstacle.entries.associateWith { obstacle -> prefs.getInt("obstacle_${obstacle.name}", 0) },
            boardState = prefs.getString("board_state", "") ?: "",
            obstacleState = prefs.getString("obstacle_state", "") ?: ""
        )
    }

    fun clearSessionBoard() { prefs.edit().remove("board_state").remove("obstacle_state").apply() }

    fun markRewardClaimed(level: Int): Boolean {
        val key = "reward_level_$level"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    fun markBuildTaskCompleted(level: Int) { prefs.edit().putBoolean("build_task_$level", true).apply() }
    fun isBuildTaskCompleted(level: Int): Boolean = prefs.getBoolean("build_task_$level", false)
}

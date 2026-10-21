package com.weearntogether.lazygarden

import android.content.Context

data class LazyGardenProgress(val level: Int, val movesLeft: Int, val collectedLeaf: Int, val collectedFlower: Int)

class LazyGardenProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("lazy_garden_progress", Context.MODE_PRIVATE)

    fun save(progress: LazyGardenProgress) = prefs.edit()
        .putInt("level", progress.level)
        .putInt("moves", progress.movesLeft)
        .putInt("leaf", progress.collectedLeaf)
        .putInt("flower", progress.collectedFlower)
        .apply()

    fun load(): LazyGardenProgress = LazyGardenProgress(
        level = prefs.getInt("level", 1),
        movesLeft = prefs.getInt("moves", LazyGardenLevels.level1.moves),
        collectedLeaf = prefs.getInt("leaf", 0),
        collectedFlower = prefs.getInt("flower", 0)
    )
}

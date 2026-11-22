package com.weearntogether.play

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Regression tests: partial cloud saves must never erase an offline puzzle round. */
class CloudProgressMergeTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val account = "cloud_merge_regression_test"

    @Test fun emptyCloudSnapshotsPreserveBothNativePuzzleSaves() {
        for ((game, file, boardKey, size) in listOf(
            Puzzle("Block Puzzle", "block_puzzle_hd2d", "board", 64),
            Puzzle("Bubble Shooter", "bubble_shooter_hd2d", "grid", 99)
        )) {
            val prefs = ProgressPreferences.forUser(context, file, account)
            try {
                val board = List(size) { if (it == 0) "1" else "0" }.joinToString(",")
                prefs.edit().clear().putString(boardKey, board).putInt("level", 7).putInt("score", 85).commit()
                val cloud = JSONObject().put(file, JSONObject())
                ProgressPreferences.apply(context, account, game, cloud)
                assertEquals("$game board", board, prefs.getString(boardKey, null))
                assertEquals("$game level", 7, prefs.getInt("level", -1))
                assertEquals("$game score", 85, prefs.getInt("score", -1))
            } finally {
                prefs.edit().clear().commit()
            }
        }
    }

    @Test fun partialCloudUpdatesMergeWithoutRemovingBoardOrOtherFields() {
        for ((game, file, boardKey, size) in listOf(
            Puzzle("Block Puzzle", "block_puzzle_hd2d", "board", 64),
            Puzzle("Bubble Shooter", "bubble_shooter_hd2d", "grid", 99)
        )) {
            val prefs = ProgressPreferences.forUser(context, file, account)
            try {
                val board = List(size) { "0" }.joinToString(",")
                prefs.edit().clear().putString(boardKey, board).putInt("level", 4)
                    .putInt("score", 30).putInt("best", 120).commit()
                ProgressPreferences.apply(context, account, game,
                    JSONObject().put(file, JSONObject().put("score", 50)))
                assertEquals("$game updated score", 50, prefs.getInt("score", -1))
                assertEquals("$game preserved board", board, prefs.getString(boardKey, null))
                assertEquals("$game preserved level", 4, prefs.getInt("level", -1))
                assertEquals("$game preserved best", 120, prefs.getInt("best", -1))
            } finally {
                prefs.edit().clear().commit()
            }
        }
    }

    private data class Puzzle(val game: String, val file: String, val boardKey: String, val size: Int)
}

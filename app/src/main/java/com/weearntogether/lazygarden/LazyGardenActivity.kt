package com.weearntogether.lazygarden

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.weearntogether.play.LevelPlayManager
import com.weearntogether.play.R

class LazyGardenActivity : Activity() {
    private val collected = mutableMapOf<GardenTile, Int>()
    private lateinit var board: LazyGardenBoardView
    private lateinit var scene: LazyGardenSceneView
    private lateinit var wallet: LazyGardenWallet
    private lateinit var progressStore: LazyGardenProgressStore
    private var currentLevel = 1
    private var unlockedLevel = 1
    private var levelFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lazy_garden)
        wallet = LazyGardenWallet(this)
        progressStore = LazyGardenProgressStore(this)
        val saved = progressStore.load()
        currentLevel = saved.level
        unlockedLevel = saved.unlockedLevel
        scene = LazyGardenSceneView(this)
        findViewById<FrameLayout>(R.id.lgSceneHost).addView(scene, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        board = LazyGardenBoardView(this)
        findViewById<FrameLayout>(R.id.lgBoardHost).addView(board, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        startLevel(currentLevel, saved.collected)
        board.onMove = { result ->
            result.tiles.forEach { (tile, count) -> collected[tile] = (collected[tile] ?: 0) + count }
            saveProgress()
            if (goalsComplete()) finishLevel(true) else if (board.engine.movesLeft <= 0) finishLevel(false) else updateHud()
        }
        findViewById<Button>(R.id.lgAction).setOnClickListener { handleAction() }
        updateHud()
    }

    private fun startLevel(number: Int, restored: Map<GardenTile, Int> = emptyMap()) {
        currentLevel = number.coerceIn(1, LazyGardenLevels.count)
        collected.clear()
        GardenTile.entries.forEach { collected[it] = restored[it] ?: 0 }
        board.loadLevel(LazyGardenLevels.get(currentLevel))
        levelFinished = false
        updateHud()
    }

    private fun goalsComplete(): Boolean = LazyGardenLevels.get(currentLevel).goals.all { goal -> (collected[goal.tile] ?: 0) >= goal.target }

    private fun finishLevel(won: Boolean) {
        levelFinished = true
        board.setInputEnabled(false)
        if (won) {
            val level = LazyGardenLevels.get(currentLevel)
            if (progressStore.markRewardClaimed(currentLevel)) wallet.credit(level.coinReward)
            unlockedLevel = maxOf(unlockedLevel, (currentLevel + 1).coerceAtMost(LazyGardenLevels.count))
            saveProgress()
            LevelPlayManager.showLevelCompleteVideo()
        }
        updateHud()
    }

    private fun handleAction() {
        if (!levelFinished) return
        if (!goalsComplete()) { startLevel(currentLevel); saveProgress(); return }
        val story = LazyGardenStory.get(currentLevel)
        if (!progressStore.isBuildTaskCompleted(currentLevel)) {
            if (wallet.spend(story.cost)) { progressStore.markBuildTaskCompleted(currentLevel); updateHud() }
            return
        }
        if (currentLevel < LazyGardenLevels.count) { startLevel(currentLevel + 1); saveProgress() }
    }

    private fun saveProgress() = progressStore.save(LazyGardenProgress(currentLevel, unlockedLevel, board.engine.movesLeft, collected.toMap()))
    private fun tileName(tile: GardenTile): String = tile.name.lowercase().replaceFirstChar { it.uppercase() }

    private fun updateHud() {
        val level = LazyGardenLevels.get(currentLevel)
        val story = LazyGardenStory.get(currentLevel)
        val taskDone = progressStore.isBuildTaskCompleted(currentLevel)
        scene.setCompletedTasks((1..LazyGardenLevels.count).filter { progressStore.isBuildTaskCompleted(it) }.toSet())
        findViewById<TextView>(R.id.lgLevel).text = getString(R.string.lg_level, currentLevel)
        findViewById<TextView>(R.id.lgMoves).text = "${getString(R.string.lg_moves)}: ${board.engine.movesLeft}"
        findViewById<TextView>(R.id.lgCoins).text = "${getString(R.string.lg_coins)}: ${wallet.coins}"
        findViewById<TextView>(R.id.lgSpeaker).text = story.speaker
        findViewById<TextView>(R.id.lgAvatar).text = story.speaker.take(1)
        findViewById<TextView>(R.id.lgDialogue).text = if (taskDone) story.after else story.before
        findViewById<TextView>(R.id.lgGoals).text = level.goals.joinToString(prefix = "${getString(R.string.lg_goal)}  •  ", separator = "  •  ") { goal -> "${tileName(goal.tile)} ${collected[goal.tile] ?: 0}/${goal.target}" }
        findViewById<TextView>(R.id.lgBuildTask).text = if (taskDone) "✓ ${story.sceneLabel} restored — ${story.buildTask}" else "Restoration: ${story.buildTask} • ${story.cost} coins"
        val action = findViewById<Button>(R.id.lgAction)
        when {
            !levelFinished -> { action.isEnabled = false; action.text = "Complete level $currentLevel to unlock restoration" }
            !goalsComplete() -> { action.isEnabled = true; action.text = "Retry level $currentLevel" }
            !taskDone -> { action.isEnabled = wallet.coins >= story.cost; action.text = "Restore ${story.sceneLabel} • ${story.cost} coins" }
            currentLevel < LazyGardenLevels.count -> { action.isEnabled = true; action.text = "Continue story • Level ${currentLevel + 1}" }
            else -> { action.isEnabled = false; action.text = "First garden chapter complete" }
        }
    }
}

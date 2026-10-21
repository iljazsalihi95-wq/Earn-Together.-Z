package com.weearntogether.lazygarden

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import com.weearntogether.play.R

class LazyGardenActivity : Activity() {
    private val collected = mutableMapOf<GardenTile, Int>()
    private lateinit var board: LazyGardenBoardView
    private lateinit var wallet: LazyGardenWallet
    private lateinit var progressStore: LazyGardenProgressStore
    private lateinit var milestone: LazyGardenMilestone
    private var rewardGranted = false
    private var levelCompletionCounted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lazy_garden)

        wallet = LazyGardenWallet(this)
        progressStore = LazyGardenProgressStore(this)
        milestone = LazyGardenMilestone(this)
        val saved = progressStore.load()
        collected[GardenTile.LEAF] = saved.collectedLeaf
        collected[GardenTile.FLOWER] = saved.collectedFlower

        board = LazyGardenBoardView(this)
        findViewById<FrameLayout>(R.id.lgBoardHost).addView(
            board,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        board.onMove = { result ->
            result.tiles.forEach { (tile, count) ->
                collected[tile] = (collected[tile] ?: 0) + count
            }
            progressStore.save(
                LazyGardenProgress(
                    level = 1,
                    movesLeft = board.engine.movesLeft,
                    collectedLeaf = collected[GardenTile.LEAF] ?: 0,
                    collectedFlower = collected[GardenTile.FLOWER] ?: 0
                )
            )
            if (goalsComplete()) {
                if (!rewardGranted) {
                    wallet.credit(LazyGardenLevels.level1.coinReward)
                    rewardGranted = true
                }
                if (!levelCompletionCounted) {
                    milestone.markLevelCompleted(1)
                    levelCompletionCounted = true
                }
            }
            updateHud()
        }
        updateHud()
    }

    private fun goalsComplete(): Boolean =
        (collected[GardenTile.LEAF] ?: 0) >= 20 && (collected[GardenTile.FLOWER] ?: 0) >= 20

    private fun updateHud() {
        findViewById<TextView>(R.id.lgLevel).text = getString(R.string.lg_level, 1)
        findViewById<TextView>(R.id.lgMoves).text = "${getString(R.string.lg_moves)}: ${board.engine.movesLeft}"
        findViewById<TextView>(R.id.lgCoins).text = "${getString(R.string.lg_coins)}: ${wallet.coins}"
        val demoProgress = "Demo reward ${milestone.completedLevels}/${milestone.targetLevels} • €1.00 pending verification"
        findViewById<TextView>(R.id.lgGoals).text =
            "${getString(R.string.lg_goal)}  •  Leaf ${collected[GardenTile.LEAF] ?: 0}/20  •  Flower ${collected[GardenTile.FLOWER] ?: 0}/20\n$demoProgress"
    }
}

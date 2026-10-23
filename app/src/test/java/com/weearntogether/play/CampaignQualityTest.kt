package com.weearntogether.play

import com.weearntogether.lazygarden.GardenObstacle
import com.weearntogether.lazygarden.LazyGardenLevels
import org.junit.Assert.*
import org.junit.Test

/** Regression gates for the native campaign. */
class CampaignQualityTest {
    @Test fun lazyGarden_hasFullPlayableCampaign() {
        assertEquals(250, LazyGardenLevels.count)
        for (level in 1..250) {
            val spec = LazyGardenLevels.get(level)
            assertEquals(level, spec.number)
            assertTrue("level $level has too few moves", spec.moves >= 10)
            assertTrue("level $level has no objective", spec.goals.isNotEmpty() || spec.obstacleGoals.isNotEmpty())
            assertTrue("level $level has invalid board", spec.rows in 6..10 && spec.columns in 6..10)
            // Coins are in-game LazyGarden progression currency only; they are not cash/redeemable rewards.
            assertTrue("level $level has invalid campaign coin value", spec.coinReward >= 0)
            spec.goals.forEach { assertTrue("level $level has invalid tile target", it.target > 0) }
            spec.obstacleGoals.forEach { assertTrue("level $level has invalid obstacle target", it.target > 0) }
        }
    }

    @Test fun lazyGarden_obstaclesProgressInsteadOfAppearingAtStart() {
        assertTrue(LazyGardenLevels.get(1).obstacleGoals.isEmpty())
        assertTrue(LazyGardenLevels.get(5).obstacleGoals.isEmpty())
        assertTrue(LazyGardenLevels.get(6).obstacleGoals.any { it.obstacle == GardenObstacle.CRATE })
        assertTrue(LazyGardenLevels.get(8).obstacleGoals.any { it.obstacle == GardenObstacle.IVY })
        assertTrue((10..250).any { n -> LazyGardenLevels.get(n).obstacleGoals.size >= 2 })
    }

    @Test fun lazyGarden_difficultyActuallyGrowsAcrossCampaign() {
        fun pressure(level:Int):Double {
            val s=LazyGardenLevels.get(level)
            val tileTargets=s.goals.sumOf { it.target }
            val obstacleTargets=s.obstacleGoals.sumOf { it.target }
            return (tileTargets + obstacleTargets * 1.5) / s.moves.toDouble()
        }
        val early=(1..10).map(::pressure).average()
        val late=(201..250).map(::pressure).average()
        assertTrue("late campaign should be more demanding than tutorial", late > early)
    }
}

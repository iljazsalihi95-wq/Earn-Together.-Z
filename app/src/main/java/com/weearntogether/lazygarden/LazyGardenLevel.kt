package com.weearntogether.lazygarden

data class LazyGardenGoal(val tile: GardenTile, val target: Int)
data class LazyGardenObstacleGoal(val obstacle: GardenObstacle, val target: Int)

data class LazyGardenLevel(
    val number: Int,
    val rows: Int,
    val columns: Int,
    val moves: Int,
    val goals: List<LazyGardenGoal>,
    val coinReward: Int,
    val obstacleGoals: List<LazyGardenObstacleGoal> = emptyList()
)

enum class GardenTile { LEAF, FLOWER, ROSE, BERRY, PEAR, WATER }
enum class GardenObstacle { CRATE, IVY, STONE }

object LazyGardenLevels {
    private const val CAMPAIGN_LEVELS = 250

    private fun level(number: Int, moves: Int, reward: Int, vararg goals: Pair<GardenTile, Int>) =
        LazyGardenLevel(number, 8, 8, moves, goals.map { LazyGardenGoal(it.first, it.second) }, reward)

    private val early = listOf(
        level(1,24,50,GardenTile.LEAF to 20,GardenTile.FLOWER to 20), level(2,23,55,GardenTile.ROSE to 18,GardenTile.LEAF to 22),
        level(3,22,60,GardenTile.BERRY to 20,GardenTile.FLOWER to 24), level(4,22,65,GardenTile.PEAR to 22,GardenTile.ROSE to 22),
        level(5,21,70,GardenTile.WATER to 20,GardenTile.LEAF to 28), level(6,21,75,GardenTile.FLOWER to 26,GardenTile.BERRY to 26),
        level(7,20,80,GardenTile.ROSE to 28,GardenTile.PEAR to 24), level(8,20,85,GardenTile.WATER to 24,GardenTile.FLOWER to 30),
        level(9,19,90,GardenTile.BERRY to 30,GardenTile.LEAF to 30), level(10,18,100,GardenTile.ROSE to 30,GardenTile.PEAR to 30,GardenTile.WATER to 20)
    )

    /** Original campaign pacing. Later chapters become harder through move pressure, mixed goals and
     * obstacles, while collection targets stay inside a realistically playable range. */
    private fun generated(number: Int): LazyGardenLevel {
        val chapter = (number - 1) / 10
        val step = (number - 1) % 10
        val tiles = GardenTile.entries
        val primary = tiles[(number + chapter) % tiles.size]
        var secondary = tiles[(number * 3 + chapter + 1) % tiles.size]
        if (secondary == primary) secondary = tiles[(tiles.indexOf(primary) + 2) % tiles.size]
        var tertiary = tiles[(number * 5 + chapter + 2) % tiles.size]
        if (tertiary == primary || tertiary == secondary) tertiary = tiles[(tiles.indexOf(secondary) + 2) % tiles.size]

        val pressure = (chapter / 5).coerceAtMost(4)
        val recovery = step == 0 || step == 5
        val challenge = step == 4 || step == 7
        val finale = step == 9
        val moves = when {
            recovery -> 24 - pressure
            finale -> 21 - pressure
            challenge -> 22 - pressure
            else -> 23 - pressure
        }.coerceAtLeast(17)

        // Keep goals achievable: difficulty comes from combining targets with blockers rather than
        // asking for hundreds of specific tiles in fewer than twenty moves.
        val chapterGrowth = (chapter / 3).coerceAtMost(8)
        val primaryTarget = 24 + chapterGrowth + step / 2
        val secondaryTarget = 20 + chapterGrowth + if (challenge || finale) 4 else step / 3
        val goals = mutableListOf(primary to primaryTarget, secondary to secondaryTarget)
        if (challenge || finale) goals += tertiary to (14 + chapterGrowth / 2 + step / 2)

        val obstacles = mutableListOf<LazyGardenObstacleGoal>()
        if (number >= 11) obstacles += LazyGardenObstacleGoal(GardenObstacle.CRATE, (4 + chapter / 3 + step / 4).coerceAtMost(12))
        if (number >= 31 && (challenge || finale || step == 2 || step == 6)) obstacles += LazyGardenObstacleGoal(GardenObstacle.IVY, (3 + chapter / 4).coerceAtMost(9))
        if (number >= 61 && (finale || step == 3 || step == 7)) obstacles += LazyGardenObstacleGoal(GardenObstacle.STONE, (2 + chapter / 5).coerceAtMost(7))

        val reward = 55 + (number - 1) * 8 + when { finale -> 80; challenge -> 35; else -> 0 }
        return LazyGardenLevel(number, 8, 8, moves, goals.map { LazyGardenGoal(it.first, it.second) }, reward, obstacles)
    }

    private val levels: List<LazyGardenLevel> = early + (11..CAMPAIGN_LEVELS).map(::generated)
    fun get(number: Int): LazyGardenLevel = levels[(number - 1).coerceIn(0, levels.lastIndex)]
    val count: Int get() = levels.size
}

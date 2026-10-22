package com.weearntogether.lazygarden

data class LazyGardenGoal(val tile: GardenTile, val target: Int)

data class LazyGardenLevel(
    val number: Int,
    val rows: Int,
    val columns: Int,
    val moves: Int,
    val goals: List<LazyGardenGoal>,
    val coinReward: Int
)

enum class GardenTile { LEAF, FLOWER, ROSE, BERRY, PEAR, WATER }

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

    /**
     * Original chapter progression inspired by the pacing of premium match-3 campaigns without
     * reproducing proprietary boards. Every ten levels form a chapter: recovery boards give the
     * player breathing room, challenge boards tighten the move budget, and chapter finales combine
     * three goals. Targets rise gradually instead of jumping only because the level number is higher.
     */
    private fun generated(number: Int): LazyGardenLevel {
        val chapter = (number - 1) / 10
        val step = (number - 1) % 10
        val tiles = GardenTile.entries
        val primary = tiles[(number + chapter) % tiles.size]
        var secondary = tiles[(number * 3 + chapter + 1) % tiles.size]
        if (secondary == primary) secondary = tiles[(tiles.indexOf(primary) + 2) % tiles.size]
        var tertiary = tiles[(number * 5 + chapter + 2) % tiles.size]
        if (tertiary == primary || tertiary == secondary) tertiary = tiles[(tiles.indexOf(secondary) + 2) % tiles.size]

        val lateGamePressure = (chapter / 4).coerceAtMost(5)
        val recovery = step == 0 || step == 5
        val challenge = step == 4 || step == 7
        val finale = step == 9
        val moves = when {
            recovery -> 24 - lateGamePressure
            finale -> 20 - lateGamePressure
            challenge -> 21 - lateGamePressure
            else -> 23 - lateGamePressure
        }.coerceAtLeast(14)

        val chapterGrowth = (chapter * 3).coerceAtMost(54)
        val base = 24 + chapterGrowth + step * 2
        val goals = mutableListOf(
            primary to base,
            secondary to (base - if (recovery) 8 else 4).coerceAtLeast(18)
        )
        if (challenge || finale) goals += tertiary to (20 + chapterGrowth / 2 + step)

        val reward = 55 + (number - 1) * 8 + when {
            finale -> 80
            challenge -> 35
            else -> 0
        }
        return level(number, moves, reward, *goals.toTypedArray())
    }

    private val levels: List<LazyGardenLevel> = early + (11..CAMPAIGN_LEVELS).map(::generated)

    fun get(number: Int): LazyGardenLevel = levels[(number - 1).coerceIn(0, levels.lastIndex)]
    val count: Int get() = levels.size
}

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
    val level1 = LazyGardenLevel(1, 8, 8, 24, listOf(
        LazyGardenGoal(GardenTile.LEAF, 20), LazyGardenGoal(GardenTile.FLOWER, 20)
    ), 50)
    val level2 = LazyGardenLevel(2, 8, 8, 23, listOf(
        LazyGardenGoal(GardenTile.ROSE, 18), LazyGardenGoal(GardenTile.LEAF, 22)
    ), 55)
    val level3 = LazyGardenLevel(3, 8, 8, 22, listOf(
        LazyGardenGoal(GardenTile.BERRY, 20), LazyGardenGoal(GardenTile.FLOWER, 24)
    ), 60)
    val level4 = LazyGardenLevel(4, 8, 8, 22, listOf(
        LazyGardenGoal(GardenTile.PEAR, 22), LazyGardenGoal(GardenTile.ROSE, 22)
    ), 65)
    val level5 = LazyGardenLevel(5, 8, 8, 21, listOf(
        LazyGardenGoal(GardenTile.WATER, 20), LazyGardenGoal(GardenTile.LEAF, 28)
    ), 70)
    val level6 = LazyGardenLevel(6, 8, 8, 21, listOf(
        LazyGardenGoal(GardenTile.FLOWER, 26), LazyGardenGoal(GardenTile.BERRY, 26)
    ), 75)
    val level7 = LazyGardenLevel(7, 8, 8, 20, listOf(
        LazyGardenGoal(GardenTile.ROSE, 28), LazyGardenGoal(GardenTile.PEAR, 24)
    ), 80)
    val level8 = LazyGardenLevel(8, 8, 8, 20, listOf(
        LazyGardenGoal(GardenTile.WATER, 24), LazyGardenGoal(GardenTile.FLOWER, 30)
    ), 85)
    val level9 = LazyGardenLevel(9, 8, 8, 19, listOf(
        LazyGardenGoal(GardenTile.BERRY, 30), LazyGardenGoal(GardenTile.LEAF, 30)
    ), 90)
    val level10 = LazyGardenLevel(10, 8, 8, 18, listOf(
        LazyGardenGoal(GardenTile.ROSE, 30), LazyGardenGoal(GardenTile.PEAR, 30), LazyGardenGoal(GardenTile.WATER, 20)
    ), 100)

    private val levels = listOf(level1, level2, level3, level4, level5, level6, level7, level8, level9, level10)

    fun get(number: Int): LazyGardenLevel = levels[(number - 1).coerceIn(0, levels.lastIndex)]
    val count: Int get() = levels.size
}

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
    val level1 = LazyGardenLevel(
        number = 1,
        rows = 8,
        columns = 8,
        moves = 24,
        goals = listOf(
            LazyGardenGoal(GardenTile.LEAF, 20),
            LazyGardenGoal(GardenTile.FLOWER, 20)
        ),
        coinReward = 50
    )
}

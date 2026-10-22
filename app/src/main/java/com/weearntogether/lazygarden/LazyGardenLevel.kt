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
    private fun level(number: Int, moves: Int, reward: Int, vararg goals: Pair<GardenTile, Int>) =
        LazyGardenLevel(number, 8, 8, moves, goals.map { LazyGardenGoal(it.first, it.second) }, reward)

    val level1 = level(1,24,50,GardenTile.LEAF to 20,GardenTile.FLOWER to 20)
    val level2 = level(2,23,55,GardenTile.ROSE to 18,GardenTile.LEAF to 22)
    val level3 = level(3,22,60,GardenTile.BERRY to 20,GardenTile.FLOWER to 24)
    val level4 = level(4,22,65,GardenTile.PEAR to 22,GardenTile.ROSE to 22)
    val level5 = level(5,21,70,GardenTile.WATER to 20,GardenTile.LEAF to 28)
    val level6 = level(6,21,75,GardenTile.FLOWER to 26,GardenTile.BERRY to 26)
    val level7 = level(7,20,80,GardenTile.ROSE to 28,GardenTile.PEAR to 24)
    val level8 = level(8,20,85,GardenTile.WATER to 24,GardenTile.FLOWER to 30)
    val level9 = level(9,19,90,GardenTile.BERRY to 30,GardenTile.LEAF to 30)
    val level10 = level(10,18,100,GardenTile.ROSE to 30,GardenTile.PEAR to 30,GardenTile.WATER to 20)
    val level11 = level(11,22,105,GardenTile.LEAF to 34,GardenTile.ROSE to 24)
    val level12 = level(12,21,110,GardenTile.FLOWER to 34,GardenTile.WATER to 24)
    val level13 = level(13,21,115,GardenTile.BERRY to 32,GardenTile.PEAR to 28)
    val level14 = level(14,20,120,GardenTile.ROSE to 34,GardenTile.FLOWER to 30)
    val level15 = level(15,20,130,GardenTile.WATER to 28,GardenTile.LEAF to 34,GardenTile.BERRY to 20)
    val level16 = level(16,20,135,GardenTile.PEAR to 34,GardenTile.ROSE to 32)
    val level17 = level(17,19,140,GardenTile.FLOWER to 36,GardenTile.BERRY to 32)
    val level18 = level(18,19,145,GardenTile.WATER to 30,GardenTile.PEAR to 32,GardenTile.LEAF to 22)
    val level19 = level(19,19,150,GardenTile.ROSE to 36,GardenTile.LEAF to 36)
    val level20 = level(20,18,165,GardenTile.BERRY to 34,GardenTile.FLOWER to 34,GardenTile.WATER to 24)
    val level21 = level(21,19,170,GardenTile.PEAR to 38,GardenTile.LEAF to 34)
    val level22 = level(22,18,175,GardenTile.ROSE to 38,GardenTile.WATER to 30)
    val level23 = level(23,18,180,GardenTile.FLOWER to 40,GardenTile.BERRY to 36,GardenTile.PEAR to 22)
    val level24 = level(24,18,190,GardenTile.LEAF to 40,GardenTile.ROSE to 36,GardenTile.WATER to 26)
    val level25 = level(25,17,210,GardenTile.PEAR to 38,GardenTile.FLOWER to 38,GardenTile.BERRY to 32)
    val level26 = level(26,19,215,GardenTile.WATER to 36,GardenTile.LEAF to 42)
    val level27 = level(27,18,220,GardenTile.ROSE to 42,GardenTile.BERRY to 38)
    val level28 = level(28,18,225,GardenTile.PEAR to 42,GardenTile.FLOWER to 40,GardenTile.WATER to 24)
    val level29 = level(29,18,230,GardenTile.LEAF to 44,GardenTile.BERRY to 40)
    val level30 = level(30,17,245,GardenTile.ROSE to 42,GardenTile.PEAR to 40,GardenTile.FLOWER to 28)
    val level31 = level(31,18,250,GardenTile.WATER to 40,GardenTile.FLOWER to 42)
    val level32 = level(32,17,255,GardenTile.BERRY to 44,GardenTile.LEAF to 42)
    val level33 = level(33,17,260,GardenTile.PEAR to 44,GardenTile.ROSE to 42,GardenTile.WATER to 28)
    val level34 = level(34,17,270,GardenTile.FLOWER to 46,GardenTile.ROSE to 44)
    val level35 = level(35,16,285,GardenTile.LEAF to 46,GardenTile.PEAR to 44,GardenTile.BERRY to 30)
    val level36 = level(36,17,290,GardenTile.WATER to 42,GardenTile.BERRY to 46)
    val level37 = level(37,16,300,GardenTile.ROSE to 46,GardenTile.LEAF to 46,GardenTile.FLOWER to 30)
    val level38 = level(38,16,310,GardenTile.PEAR to 46,GardenTile.WATER to 42)
    val level39 = level(39,16,325,GardenTile.FLOWER to 48,GardenTile.BERRY to 46,GardenTile.ROSE to 32)
    val level40 = level(40,15,350,GardenTile.LEAF to 48,GardenTile.PEAR to 48,GardenTile.WATER to 36)

    // Chapters 8-9: alternating pressure levels reward planning instead of simply inflating every target.
    val level41 = level(41,18,355,GardenTile.ROSE to 48,GardenTile.FLOWER to 46)
    val level42 = level(42,17,360,GardenTile.WATER to 44,GardenTile.LEAF to 48,GardenTile.BERRY to 28)
    val level43 = level(43,17,370,GardenTile.PEAR to 48,GardenTile.BERRY to 46)
    val level44 = level(44,16,380,GardenTile.FLOWER to 50,GardenTile.ROSE to 46,GardenTile.WATER to 30)
    val level45 = level(45,16,400,GardenTile.LEAF to 50,GardenTile.PEAR to 48,GardenTile.BERRY to 34)
    val level46 = level(46,18,405,GardenTile.WATER to 46,GardenTile.ROSE to 48)
    val level47 = level(47,17,415,GardenTile.BERRY to 50,GardenTile.FLOWER to 48,GardenTile.LEAF to 30)
    val level48 = level(48,16,425,GardenTile.PEAR to 50,GardenTile.WATER to 46)
    val level49 = level(49,16,440,GardenTile.ROSE to 50,GardenTile.LEAF to 50,GardenTile.BERRY to 34)
    val level50 = level(50,15,470,GardenTile.FLOWER to 52,GardenTile.PEAR to 50,GardenTile.WATER to 38)

    // Chapters 10-11: milestone boards tighten moves while keeping targets achievable through cascades.
    val level51 = level(51,18,475,GardenTile.LEAF to 52,GardenTile.BERRY to 48)
    val level52 = level(52,17,485,GardenTile.ROSE to 52,GardenTile.WATER to 48,GardenTile.FLOWER to 30)
    val level53 = level(53,17,495,GardenTile.PEAR to 52,GardenTile.FLOWER to 50)
    val level54 = level(54,16,510,GardenTile.BERRY to 52,GardenTile.LEAF to 50,GardenTile.WATER to 34)
    val level55 = level(55,16,530,GardenTile.ROSE to 54,GardenTile.PEAR to 50,GardenTile.FLOWER to 36)
    val level56 = level(56,17,540,GardenTile.WATER to 50,GardenTile.BERRY to 52)
    val level57 = level(57,16,550,GardenTile.FLOWER to 54,GardenTile.LEAF to 52,GardenTile.ROSE to 34)
    val level58 = level(58,16,565,GardenTile.PEAR to 54,GardenTile.WATER to 50)
    val level59 = level(59,15,580,GardenTile.BERRY to 54,GardenTile.ROSE to 52,GardenTile.LEAF to 38)
    val level60 = level(60,15,620,GardenTile.FLOWER to 56,GardenTile.PEAR to 54,GardenTile.WATER to 40)

    private val levels = listOf(
        level1,level2,level3,level4,level5,level6,level7,level8,level9,level10,
        level11,level12,level13,level14,level15,level16,level17,level18,level19,level20,
        level21,level22,level23,level24,level25,level26,level27,level28,level29,level30,
        level31,level32,level33,level34,level35,level36,level37,level38,level39,level40,
        level41,level42,level43,level44,level45,level46,level47,level48,level49,level50,
        level51,level52,level53,level54,level55,level56,level57,level58,level59,level60
    )

    fun get(number: Int): LazyGardenLevel = levels[(number - 1).coerceIn(0, levels.lastIndex)]
    val count: Int get() = levels.size
}

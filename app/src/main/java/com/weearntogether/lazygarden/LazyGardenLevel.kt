package com.weearntogether.lazygarden

data class LazyGardenGoal(val tile: GardenTile, val target: Int)
data class LazyGardenObstacleGoal(val obstacle: GardenObstacle, val target: Int)

data class LazyGardenLevel(
    val number: Int, val rows: Int, val columns: Int, val moves: Int,
    val goals: List<LazyGardenGoal>, val coinReward: Int,
    val obstacleGoals: List<LazyGardenObstacleGoal> = emptyList()
)

enum class GardenTile { LEAF, FLOWER, ROSE, BERRY, PEAR, WATER }
enum class GardenObstacle { CRATE, IVY, STONE }

object LazyGardenLevels {
    private const val CAMPAIGN_LEVELS = 250
    private fun level(number:Int,moves:Int,reward:Int,vararg goals:Pair<GardenTile,Int>) =
        LazyGardenLevel(number,8,8,moves,goals.map { LazyGardenGoal(it.first,it.second) },reward)
    private fun obstacle(type:GardenObstacle,target:Int)=LazyGardenObstacleGoal(type,target)

    /** Hand-authored opening campaign. Chapters deliberately teach one mechanic at a time before
     * mixing it with tighter move budgets. No proprietary level layouts or assets are used. */
    private val authored = listOf(
        level(1,24,50,GardenTile.LEAF to 20,GardenTile.FLOWER to 20),
        level(2,23,55,GardenTile.ROSE to 18,GardenTile.LEAF to 22),
        level(3,22,60,GardenTile.BERRY to 20,GardenTile.FLOWER to 24),
        level(4,22,65,GardenTile.PEAR to 22,GardenTile.ROSE to 22),
        level(5,21,70,GardenTile.WATER to 20,GardenTile.LEAF to 28),
        level(6,23,75,GardenTile.FLOWER to 22,GardenTile.BERRY to 22).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,3))),
        level(7,22,80,GardenTile.ROSE to 24,GardenTile.PEAR to 22).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,4))),
        level(8,23,85,GardenTile.WATER to 22,GardenTile.FLOWER to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,3))),
        level(9,22,90,GardenTile.BERRY to 24,GardenTile.LEAF to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,4))),
        level(10,23,100,GardenTile.ROSE to 24,GardenTile.PEAR to 24,GardenTile.WATER to 14).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,4),obstacle(GardenObstacle.IVY,3))),
        level(11,24,105,GardenTile.LEAF to 24,GardenTile.PEAR to 20).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,4))),
        level(12,23,110,GardenTile.FLOWER to 24,GardenTile.WATER to 20).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,4))),
        level(13,22,115,GardenTile.ROSE to 25,GardenTile.BERRY to 22).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,5))),
        level(14,22,120,GardenTile.PEAR to 24,GardenTile.LEAF to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,3),obstacle(GardenObstacle.IVY,3))),
        level(15,24,125,GardenTile.WATER to 26,GardenTile.FLOWER to 22).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,5))),
        level(16,22,130,GardenTile.BERRY to 26,GardenTile.ROSE to 22).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,5),obstacle(GardenObstacle.IVY,3))),
        level(17,21,135,GardenTile.LEAF to 27,GardenTile.WATER to 23).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,6))),
        level(18,22,145,GardenTile.FLOWER to 26,GardenTile.PEAR to 24,GardenTile.BERRY to 14).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,5))),
        level(19,21,150,GardenTile.ROSE to 27,GardenTile.LEAF to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,5),obstacle(GardenObstacle.IVY,4))),
        level(20,23,175,GardenTile.WATER to 26,GardenTile.BERRY to 24,GardenTile.FLOWER to 16).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,6),obstacle(GardenObstacle.IVY,5))),
        level(21,24,180,GardenTile.LEAF to 26,GardenTile.ROSE to 22).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,5))),
        level(22,23,185,GardenTile.PEAR to 25,GardenTile.WATER to 23).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,5))),
        level(23,22,190,GardenTile.FLOWER to 27,GardenTile.BERRY to 23).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,4),obstacle(GardenObstacle.IVY,4))),
        level(24,21,195,GardenTile.ROSE to 28,GardenTile.LEAF to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,6),obstacle(GardenObstacle.IVY,3))),
        level(25,24,205,GardenTile.WATER to 27,GardenTile.PEAR to 24,GardenTile.FLOWER to 14).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,5))),
        level(26,22,210,GardenTile.BERRY to 28,GardenTile.ROSE to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,6),obstacle(GardenObstacle.IVY,4))),
        level(27,21,215,GardenTile.LEAF to 29,GardenTile.WATER to 24,GardenTile.PEAR to 14).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,7))),
        level(28,22,225,GardenTile.FLOWER to 28,GardenTile.BERRY to 25).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,6),obstacle(GardenObstacle.CRATE,4))),
        level(29,21,235,GardenTile.ROSE to 29,GardenTile.PEAR to 25,GardenTile.LEAF to 15).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,6),obstacle(GardenObstacle.IVY,5))),
        level(30,23,260,GardenTile.WATER to 30,GardenTile.FLOWER to 26,GardenTile.BERRY to 16).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,7),obstacle(GardenObstacle.IVY,6))),
        level(31,24,265,GardenTile.LEAF to 28,GardenTile.FLOWER to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,6))),
        level(32,23,270,GardenTile.ROSE to 27,GardenTile.WATER to 25).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,5),obstacle(GardenObstacle.IVY,4))),
        level(33,22,275,GardenTile.BERRY to 29,GardenTile.PEAR to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,7))),
        level(34,21,285,GardenTile.FLOWER to 29,GardenTile.LEAF to 25,GardenTile.ROSE to 14).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,6),obstacle(GardenObstacle.IVY,5))),
        level(35,24,290,GardenTile.WATER to 28,GardenTile.BERRY to 25).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,7))),
        level(36,22,300,GardenTile.PEAR to 29,GardenTile.ROSE to 25).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,7),obstacle(GardenObstacle.IVY,4))),
        level(37,21,305,GardenTile.LEAF to 30,GardenTile.WATER to 25,GardenTile.BERRY to 15).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,8))),
        level(38,22,315,GardenTile.FLOWER to 30,GardenTile.PEAR to 26).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,6),obstacle(GardenObstacle.IVY,6))),
        level(39,21,325,GardenTile.ROSE to 30,GardenTile.BERRY to 26,GardenTile.LEAF to 15).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,7),obstacle(GardenObstacle.IVY,6))),
        level(40,23,350,GardenTile.WATER to 30,GardenTile.FLOWER to 27,GardenTile.PEAR to 16).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,8),obstacle(GardenObstacle.IVY,7))),

        // Chapter 5: first Stone encounters. Stone is introduced alone, then paired carefully with
        // familiar blockers so the new rule is learned before the chapter finale mixes all three.
        level(41,24,355,GardenTile.LEAF to 27,GardenTile.ROSE to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.STONE,2))),
        level(42,23,360,GardenTile.FLOWER to 28,GardenTile.WATER to 24).copy(obstacleGoals=listOf(obstacle(GardenObstacle.STONE,3))),
        level(43,23,370,GardenTile.BERRY to 28,GardenTile.PEAR to 25).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,5),obstacle(GardenObstacle.STONE,2))),
        level(44,22,380,GardenTile.ROSE to 29,GardenTile.LEAF to 25).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,5),obstacle(GardenObstacle.STONE,2))),
        level(45,24,390,GardenTile.WATER to 28,GardenTile.FLOWER to 25,GardenTile.BERRY to 14).copy(obstacleGoals=listOf(obstacle(GardenObstacle.STONE,3))),
        level(46,22,400,GardenTile.PEAR to 29,GardenTile.ROSE to 26).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,6),obstacle(GardenObstacle.STONE,3))),
        level(47,21,410,GardenTile.LEAF to 30,GardenTile.WATER to 26,GardenTile.FLOWER to 14).copy(obstacleGoals=listOf(obstacle(GardenObstacle.IVY,6),obstacle(GardenObstacle.STONE,3))),
        level(48,22,425,GardenTile.BERRY to 30,GardenTile.PEAR to 26).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,6),obstacle(GardenObstacle.IVY,4),obstacle(GardenObstacle.STONE,2))),
        level(49,21,440,GardenTile.ROSE to 30,GardenTile.FLOWER to 27,GardenTile.LEAF to 15).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,7),obstacle(GardenObstacle.STONE,4))),
        level(50,23,475,GardenTile.WATER to 31,GardenTile.BERRY to 27,GardenTile.PEAR to 16).copy(obstacleGoals=listOf(obstacle(GardenObstacle.CRATE,7),obstacle(GardenObstacle.IVY,6),obstacle(GardenObstacle.STONE,4)))
    )

    private fun generated(number:Int):LazyGardenLevel {
        val chapter=(number-1)/10
        val step=(number-1)%10
        val tiles=GardenTile.entries
        val primary=tiles[(number+chapter)%tiles.size]
        val afterPrimary=tiles.filter { it!=primary }
        val secondary=afterPrimary[(number*3+chapter+1)%afterPrimary.size]
        val afterSecondary=afterPrimary.filter { it!=secondary }
        val tertiary=afterSecondary[(number*5+chapter+2)%afterSecondary.size]
        val pressure=(chapter/5).coerceAtMost(4)
        val recovery=step==0||step==5
        val challenge=step==4||step==7
        val finale=step==9
        val moves=when { recovery->24-pressure; finale->21-pressure; challenge->22-pressure; else->23-pressure }.coerceAtLeast(17)
        val growth=(chapter/3).coerceAtMost(8)
        val goals=mutableListOf(primary to (24+growth+step/2),secondary to (20+growth+if(challenge||finale)4 else step/3))
        if(challenge||finale) goals+=tertiary to (14+growth/2+step/2)
        val obstacles=mutableListOf<LazyGardenObstacleGoal>()
        obstacles+=obstacle(GardenObstacle.CRATE,(4+chapter/3+step/4).coerceAtMost(12))
        if(challenge||finale||step==2||step==6) obstacles+=obstacle(GardenObstacle.IVY,(3+chapter/4).coerceAtMost(9))
        if(number>=51&&(finale||step==3||step==7)) obstacles+=obstacle(GardenObstacle.STONE,(2+chapter/5).coerceAtMost(7))
        val reward=55+(number-1)*8+when { finale->80; challenge->35; else->0 }
        return LazyGardenLevel(number,8,8,moves,goals.map { LazyGardenGoal(it.first,it.second) },reward,obstacles)
    }

    private val levels:List<LazyGardenLevel> = authored + (51..CAMPAIGN_LEVELS).map(::generated)
    fun get(number:Int):LazyGardenLevel=levels[(number-1).coerceIn(0,levels.lastIndex)]
    val count:Int get()=levels.size
}

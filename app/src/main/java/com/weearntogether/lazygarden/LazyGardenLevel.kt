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
    private fun level(number:Int,moves:Int,reward:Int,vararg goals:Pair<GardenTile,Int>) = LazyGardenLevel(number,8,8,moves,goals.map { LazyGardenGoal(it.first,it.second) },reward)
    private fun obstacle(type:GardenObstacle,target:Int)=LazyGardenObstacleGoal(type,target)

    private val authored = buildList {
        fun addStage(n:Int,m:Int,r:Int,a:Pair<GardenTile,Int>,b:Pair<GardenTile,Int>,c:Pair<GardenTile,Int>?=null,vararg obs:Pair<GardenObstacle,Int>) {
            val goals=if(c==null) listOf(a,b) else listOf(a,b,c)
            add(LazyGardenLevel(n,8,8,m,goals.map{LazyGardenGoal(it.first,it.second)},r,obs.map{obstacle(it.first,it.second)}))
        }
        val chapters = listOf(
            1 to arrayOf(GardenTile.LEAF,GardenTile.FLOWER,GardenTile.ROSE,GardenTile.BERRY,GardenTile.PEAR,GardenTile.WATER),
            2 to arrayOf(GardenTile.LEAF,GardenTile.PEAR,GardenTile.FLOWER,GardenTile.WATER,GardenTile.BERRY,GardenTile.ROSE),
            3 to arrayOf(GardenTile.FLOWER,GardenTile.BERRY,GardenTile.ROSE,GardenTile.LEAF,GardenTile.WATER,GardenTile.PEAR),
            4 to arrayOf(GardenTile.LEAF,GardenTile.ROSE,GardenTile.BERRY,GardenTile.PEAR,GardenTile.FLOWER,GardenTile.WATER),
            5 to arrayOf(GardenTile.LEAF,GardenTile.FLOWER,GardenTile.BERRY,GardenTile.ROSE,GardenTile.PEAR,GardenTile.WATER),
            6 to arrayOf(GardenTile.LEAF,GardenTile.PEAR,GardenTile.FLOWER,GardenTile.BERRY,GardenTile.ROSE,GardenTile.WATER)
        )
        var n=1
        chapters.forEach { (chapter,t) ->
            repeat(10){ step ->
                val finale=step==9; val pressure=step==4||step==7||finale
                val moves=when{finale->23; pressure->21+(chapter%2); else->24-(step%3)}
                val base=18+chapter*2+step/2
                val goals=mutableListOf(t[(step+chapter)%6] to (base+4),t[(step*2+1)%6] to (base+2))
                if(pressure) goals+=t[(step*3+2)%6] to (12+chapter)
                val obs=mutableListOf<Pair<GardenObstacle,Int>>()
                if(n>=6) obs+=GardenObstacle.CRATE to (2+chapter+step/4)
                if(n>=8&&(step%2==0||pressure)) obs+=GardenObstacle.IVY to (2+chapter/2+step/5)
                if(n>=41&&(step==1||step==4||step==7||finale)) obs+=GardenObstacle.STONE to (2+(chapter-4).coerceAtLeast(0)+if(finale)1 else 0)
                addStage(n,moves,45+n*9,goals[0],goals[1],goals.getOrNull(2),*obs.toTypedArray()); n++
            }
        }
        addStage(61,24,630,GardenTile.PEAR to 30,GardenTile.LEAF to 27,null,GardenObstacle.CRATE to 7,GardenObstacle.STONE to 3)
        addStage(62,23,640,GardenTile.FLOWER to 30,GardenTile.BERRY to 27,null,GardenObstacle.IVY to 7,GardenObstacle.STONE to 3)
        addStage(63,22,650,GardenTile.ROSE to 31,GardenTile.WATER to 27,null,GardenObstacle.CRATE to 7,GardenObstacle.IVY to 5)
        addStage(64,23,665,GardenTile.LEAF to 31,GardenTile.PEAR to 27,GardenTile.FLOWER to 14,GardenObstacle.CRATE to 6,GardenObstacle.STONE to 4)
        addStage(65,21,680,GardenTile.WATER to 31,GardenTile.BERRY to 28,GardenTile.ROSE to 15,GardenObstacle.IVY to 7,GardenObstacle.STONE to 4)
        addStage(66,24,690,GardenTile.BERRY to 31,GardenTile.FLOWER to 28,null,GardenObstacle.CRATE to 8,GardenObstacle.IVY to 5)
        addStage(67,22,705,GardenTile.LEAF to 32,GardenTile.WATER to 28,GardenTile.PEAR to 15,GardenObstacle.CRATE to 8,GardenObstacle.STONE to 4)
        addStage(68,21,720,GardenTile.FLOWER to 32,GardenTile.ROSE to 28,GardenTile.BERRY to 16,GardenObstacle.IVY to 8,GardenObstacle.STONE to 4)
        addStage(69,22,735,GardenTile.ROSE to 33,GardenTile.PEAR to 28,null,GardenObstacle.CRATE to 8,GardenObstacle.IVY to 6,GardenObstacle.STONE to 4)
        addStage(70,23,780,GardenTile.WATER to 33,GardenTile.LEAF to 29,GardenTile.FLOWER to 17,GardenObstacle.CRATE to 9,GardenObstacle.IVY to 7,GardenObstacle.STONE to 5)
        addStage(71,24,790,GardenTile.LEAF to 31,GardenTile.BERRY to 28,null,GardenObstacle.CRATE to 8,GardenObstacle.STONE to 4)
        addStage(72,23,805,GardenTile.PEAR to 31,GardenTile.FLOWER to 29,null,GardenObstacle.IVY to 8,GardenObstacle.STONE to 4)
        addStage(73,22,820,GardenTile.WATER to 32,GardenTile.ROSE to 29,GardenTile.LEAF to 15,GardenObstacle.CRATE to 8,GardenObstacle.IVY to 6)
        addStage(74,21,840,GardenTile.BERRY to 33,GardenTile.PEAR to 29,GardenTile.FLOWER to 16,GardenObstacle.CRATE to 8,GardenObstacle.STONE to 5)
        addStage(75,22,860,GardenTile.ROSE to 33,GardenTile.WATER to 30,GardenTile.BERRY to 16,GardenObstacle.IVY to 8,GardenObstacle.STONE to 5)
        addStage(76,24,875,GardenTile.FLOWER to 32,GardenTile.LEAF to 29,null,GardenObstacle.CRATE to 9,GardenObstacle.IVY to 6)
        addStage(77,22,895,GardenTile.PEAR to 34,GardenTile.BERRY to 30,GardenTile.WATER to 16,GardenObstacle.CRATE to 9,GardenObstacle.STONE to 5)
        addStage(78,21,915,GardenTile.WATER to 34,GardenTile.FLOWER to 30,GardenTile.ROSE to 17,GardenObstacle.IVY to 9,GardenObstacle.STONE to 5)
        addStage(79,22,940,GardenTile.LEAF to 35,GardenTile.ROSE to 30,GardenTile.PEAR to 17,GardenObstacle.CRATE to 9,GardenObstacle.IVY to 7,GardenObstacle.STONE to 5)
        addStage(80,23,990,GardenTile.BERRY to 35,GardenTile.WATER to 31,GardenTile.FLOWER to 18,GardenObstacle.CRATE to 10,GardenObstacle.IVY to 8,GardenObstacle.STONE to 6)
        // Chapter 9 — premium campaign pacing: recovery stages alternate with dense blocker challenges.
        addStage(81,24,1000,GardenTile.ROSE to 32,GardenTile.PEAR to 29,null,GardenObstacle.CRATE to 8,GardenObstacle.STONE to 5)
        addStage(82,23,1015,GardenTile.WATER to 33,GardenTile.LEAF to 29,null,GardenObstacle.IVY to 8,GardenObstacle.STONE to 5)
        addStage(83,22,1035,GardenTile.FLOWER to 33,GardenTile.BERRY to 30,GardenTile.ROSE to 16,GardenObstacle.CRATE to 9,GardenObstacle.IVY to 6)
        addStage(84,21,1055,GardenTile.PEAR to 34,GardenTile.WATER to 30,GardenTile.LEAF to 17,GardenObstacle.CRATE to 9,GardenObstacle.STONE to 6)
        addStage(85,22,1080,GardenTile.BERRY to 34,GardenTile.ROSE to 31,GardenTile.FLOWER to 17,GardenObstacle.IVY to 9,GardenObstacle.STONE to 6)
        addStage(86,24,1095,GardenTile.LEAF to 33,GardenTile.PEAR to 30,null,GardenObstacle.CRATE to 9,GardenObstacle.IVY to 7)
        addStage(87,22,1120,GardenTile.WATER to 35,GardenTile.BERRY to 31,GardenTile.ROSE to 17,GardenObstacle.CRATE to 10,GardenObstacle.STONE to 6)
        addStage(88,21,1145,GardenTile.FLOWER to 35,GardenTile.LEAF to 31,GardenTile.PEAR to 18,GardenObstacle.IVY to 10,GardenObstacle.STONE to 6)
        addStage(89,22,1170,GardenTile.ROSE to 36,GardenTile.WATER to 32,GardenTile.BERRY to 18,GardenObstacle.CRATE to 10,GardenObstacle.IVY to 8,GardenObstacle.STONE to 6)
        addStage(90,23,1225,GardenTile.PEAR to 36,GardenTile.FLOWER to 32,GardenTile.LEAF to 19,GardenObstacle.CRATE to 11,GardenObstacle.IVY to 9,GardenObstacle.STONE to 7)
    }

    private fun generated(number:Int):LazyGardenLevel {
        val chapter=(number-1)/10; val step=(number-1)%10; val tiles=GardenTile.entries
        val primary=tiles[(number+chapter)%tiles.size]; val afterPrimary=tiles.filter{it!=primary}; val secondary=afterPrimary[(number*3+chapter+1)%afterPrimary.size]
        val afterSecondary=afterPrimary.filter{it!=secondary}; val tertiary=afterSecondary[(number*5+chapter+2)%afterSecondary.size]
        val pressure=(chapter/5).coerceAtMost(4); val recovery=step==0||step==5; val challenge=step==4||step==7; val finale=step==9
        val moves=when{recovery->24-pressure;finale->21-pressure;challenge->22-pressure;else->23-pressure}.coerceAtLeast(17); val growth=(chapter/3).coerceAtMost(8)
        val goals=mutableListOf(primary to (24+growth+step/2),secondary to (20+growth+if(challenge||finale)4 else step/3)); if(challenge||finale) goals+=tertiary to (14+growth/2+step/2)
        val obstacles=mutableListOf<LazyGardenObstacleGoal>(); obstacles+=obstacle(GardenObstacle.CRATE,(4+chapter/3+step/4).coerceAtMost(12))
        if(challenge||finale||step==2||step==6) obstacles+=obstacle(GardenObstacle.IVY,(3+chapter/4).coerceAtMost(9)); if(finale||step==3||step==7) obstacles+=obstacle(GardenObstacle.STONE,(2+chapter/5).coerceAtMost(7))
        val reward=55+(number-1)*8+when{finale->80;challenge->35;else->0}; return LazyGardenLevel(number,8,8,moves,goals.map{LazyGardenGoal(it.first,it.second)},reward,obstacles)
    }
    private val levels=authored+(91..CAMPAIGN_LEVELS).map(::generated)
    fun get(number:Int):LazyGardenLevel=levels[(number-1).coerceIn(0,levels.lastIndex)]
    val count:Int get()=levels.size
}
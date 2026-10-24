package com.weearntogether.lazygarden

data class LazyGardenGoal(val tile: GardenTile, val target: Int)
data class LazyGardenObstacleGoal(val obstacle: GardenObstacle, val target: Int)

data class LazyGardenLevel(val number:Int,val rows:Int,val columns:Int,val moves:Int,val goals:List<LazyGardenGoal>,val coinReward:Int,val obstacleGoals:List<LazyGardenObstacleGoal> = emptyList())
enum class GardenTile { LEAF, FLOWER, ROSE, BERRY, PEAR, WATER }
enum class GardenObstacle { CRATE, IVY, STONE }

object LazyGardenLevels {
 private const val CAMPAIGN_LEVELS=250
 private fun obstacle(type:GardenObstacle,target:Int)=LazyGardenObstacleGoal(type,target)
 private val authored=buildList {
  fun addStage(n:Int,m:Int,r:Int,a:Pair<GardenTile,Int>,b:Pair<GardenTile,Int>,c:Pair<GardenTile,Int>?=null,vararg obs:Pair<GardenObstacle,Int>){val g=if(c==null)listOf(a,b)else listOf(a,b,c);add(LazyGardenLevel(n,8,8,m,g.map{LazyGardenGoal(it.first,it.second)},r,obs.map{obstacle(it.first,it.second)}))}
  val chapters=listOf(
   arrayOf(GardenTile.LEAF,GardenTile.FLOWER,GardenTile.ROSE,GardenTile.BERRY,GardenTile.PEAR,GardenTile.WATER),arrayOf(GardenTile.LEAF,GardenTile.PEAR,GardenTile.FLOWER,GardenTile.WATER,GardenTile.BERRY,GardenTile.ROSE),arrayOf(GardenTile.FLOWER,GardenTile.BERRY,GardenTile.ROSE,GardenTile.LEAF,GardenTile.WATER,GardenTile.PEAR),arrayOf(GardenTile.LEAF,GardenTile.ROSE,GardenTile.BERRY,GardenTile.PEAR,GardenTile.FLOWER,GardenTile.WATER),arrayOf(GardenTile.LEAF,GardenTile.FLOWER,GardenTile.BERRY,GardenTile.ROSE,GardenTile.PEAR,GardenTile.WATER),arrayOf(GardenTile.LEAF,GardenTile.PEAR,GardenTile.FLOWER,GardenTile.BERRY,GardenTile.ROSE,GardenTile.WATER))
  var n=1;chapters.forEachIndexed{ci,t->val chapter=ci+1;repeat(10){step->val finale=step==9;val pressure=step==4||step==7||finale;val moves=when{finale->23;pressure->21+(chapter%2);else->24-(step%3)};val base=18+chapter*2+step/2;val goals=mutableListOf(t[(step+chapter)%6] to(base+4),t[(step*2+1)%6] to(base+2));if(pressure)goals+=t[(step*3+2)%6] to(12+chapter);val obs=mutableListOf<Pair<GardenObstacle,Int>>();if(n>=6)obs+=GardenObstacle.CRATE to(2+chapter+step/4);if(n>=8&&(step%2==0||pressure))obs+=GardenObstacle.IVY to(2+chapter/2+step/5);if(n>=41&&(step==1||step==4||step==7||finale))obs+=GardenObstacle.STONE to(2+(chapter-4).coerceAtLeast(0)+if(finale)1 else 0);addStage(n,moves,45+n*9,goals[0],goals[1],goals.getOrNull(2),*obs.toTypedArray());n++}}
  val authored61to90=listOf(
   Triple(61,24,630),Triple(62,23,640),Triple(63,22,650),Triple(64,23,665),Triple(65,21,680),Triple(66,24,690),Triple(67,22,705),Triple(68,21,720),Triple(69,22,735),Triple(70,23,780),Triple(71,24,790),Triple(72,23,805),Triple(73,22,820),Triple(74,21,840),Triple(75,22,860),Triple(76,24,875),Triple(77,22,895),Triple(78,21,915),Triple(79,22,940),Triple(80,23,990),Triple(81,24,1000),Triple(82,23,1015),Triple(83,22,1035),Triple(84,21,1055),Triple(85,22,1080),Triple(86,24,1095),Triple(87,22,1120),Triple(88,21,1145),Triple(89,22,1170),Triple(90,23,1225))
  val tiles=GardenTile.entries
  authored61to90.forEach{(num,moves,reward)->val step=(num-61)%10;val chapter=(num-1)/10;val a=tiles[(num+1)%6] to(30+chapter/2+step/2);val b=tiles[(num*2+3)%6] to(27+chapter/3+step/3);val c=if(step in setOf(3,4,6,7,8,9)) tiles[(num*3+2)%6] to(14+chapter/3+step/3) else null;val obs=mutableListOf<Pair<GardenObstacle,Int>>(GardenObstacle.CRATE to(7+chapter/4+step/4));if(step%2==0||step>=7)obs+=GardenObstacle.IVY to(5+chapter/5+step/5);if(step in setOf(0,1,3,4,6,7,8,9))obs+=GardenObstacle.STONE to(3+chapter/5+if(step==9)2 else 0);addStage(num,moves,reward,a,b,c,*obs.toTypedArray())}
  // Chapter 10: individually tuned late-campaign introduction with tighter move budgets and mixed blockers.
  addStage(91,24,1240,GardenTile.WATER to 34,GardenTile.BERRY to 31,null,GardenObstacle.CRATE to 9,GardenObstacle.STONE to 5)
  addStage(92,23,1260,GardenTile.LEAF to 34,GardenTile.ROSE to 31,null,GardenObstacle.IVY to 9,GardenObstacle.STONE to 5)
  addStage(93,22,1285,GardenTile.PEAR to 35,GardenTile.FLOWER to 31,GardenTile.WATER to 17,GardenObstacle.CRATE to 10,GardenObstacle.IVY to 7)
  addStage(94,21,1310,GardenTile.BERRY to 35,GardenTile.LEAF to 32,GardenTile.ROSE to 18,GardenObstacle.CRATE to 10,GardenObstacle.STONE to 6)
  addStage(95,21,1340,GardenTile.ROSE to 36,GardenTile.PEAR to 32,GardenTile.FLOWER to 18,GardenObstacle.IVY to 10,GardenObstacle.STONE to 6)
  addStage(96,24,1360,GardenTile.FLOWER to 34,GardenTile.WATER to 31,null,GardenObstacle.CRATE to 10,GardenObstacle.IVY to 8)
  addStage(97,22,1390,GardenTile.LEAF to 36,GardenTile.BERRY to 33,GardenTile.PEAR to 18,GardenObstacle.CRATE to 11,GardenObstacle.STONE to 6)
  addStage(98,20,1420,GardenTile.WATER to 37,GardenTile.ROSE to 33,GardenTile.LEAF to 19,GardenObstacle.IVY to 11,GardenObstacle.STONE to 6)
  addStage(99,21,1455,GardenTile.PEAR to 37,GardenTile.FLOWER to 34,GardenTile.BERRY to 19,GardenObstacle.CRATE to 11,GardenObstacle.IVY to 9,GardenObstacle.STONE to 6)
  addStage(100,22,1525,GardenTile.ROSE to 38,GardenTile.WATER to 34,GardenTile.FLOWER to 20,GardenObstacle.CRATE to 12,GardenObstacle.IVY to 10,GardenObstacle.STONE to 7)
  // Chapter 11: authored mastery set. Alternates recovery and pressure levels before a three-blocker finale.
  addStage(101,24,1540,GardenTile.LEAF to 35,GardenTile.PEAR to 32,null,GardenObstacle.CRATE to 9,GardenObstacle.IVY to 7)
  addStage(102,23,1565,GardenTile.FLOWER to 35,GardenTile.WATER to 32,null,GardenObstacle.STONE to 6,GardenObstacle.IVY to 8)
  addStage(103,22,1590,GardenTile.BERRY to 36,GardenTile.ROSE to 32,GardenTile.LEAF to 18,GardenObstacle.CRATE to 10,GardenObstacle.STONE to 6)
  addStage(104,21,1620,GardenTile.PEAR to 36,GardenTile.FLOWER to 33,GardenTile.WATER to 18,GardenObstacle.CRATE to 10,GardenObstacle.IVY to 9)
  addStage(105,20,1650,GardenTile.ROSE to 37,GardenTile.BERRY to 33,GardenTile.PEAR to 19,GardenObstacle.IVY to 10,GardenObstacle.STONE to 7)
  addStage(106,24,1675,GardenTile.WATER to 35,GardenTile.LEAF to 32,null,GardenObstacle.CRATE to 10,GardenObstacle.IVY to 8)
  addStage(107,22,1710,GardenTile.FLOWER to 37,GardenTile.PEAR to 34,GardenTile.ROSE to 19,GardenObstacle.CRATE to 11,GardenObstacle.STONE to 7)
  addStage(108,20,1745,GardenTile.BERRY to 38,GardenTile.WATER to 34,GardenTile.FLOWER to 20,GardenObstacle.IVY to 11,GardenObstacle.STONE to 7)
  addStage(109,21,1780,GardenTile.LEAF to 38,GardenTile.ROSE to 35,GardenTile.BERRY to 20,GardenObstacle.CRATE to 12,GardenObstacle.IVY to 10,GardenObstacle.STONE to 7)
  addStage(110,22,1850,GardenTile.PEAR to 39,GardenTile.FLOWER to 35,GardenTile.WATER to 21,GardenObstacle.CRATE to 12,GardenObstacle.IVY to 11,GardenObstacle.STONE to 8)
 }
 private fun generated(number:Int):LazyGardenLevel{val chapter=(number-1)/10;val step=(number-1)%10;val tiles=GardenTile.entries;val primary=tiles[(number+chapter)%6];val secondary=tiles.filter{it!=primary}[(number*3+chapter+1)%5];val tertiary=tiles.filter{it!=primary&&it!=secondary}[(number*5+chapter+2)%4];val pressure=(chapter/5).coerceAtMost(4);val recovery=step==0||step==5;val challenge=step==4||step==7;val finale=step==9;val moves=when{recovery->24-pressure;finale->21-pressure;challenge->22-pressure;else->23-pressure}.coerceAtLeast(17);val growth=(chapter/3).coerceAtMost(8);val goals=mutableListOf(primary to(24+growth+step/2),secondary to(20+growth+if(challenge||finale)4 else step/3));if(challenge||finale)goals+=tertiary to(14+growth/2+step/2);val obs=mutableListOf(obstacle(GardenObstacle.CRATE,(4+chapter/3+step/4).coerceAtMost(12)));if(challenge||finale||step==2||step==6)obs+=obstacle(GardenObstacle.IVY,(3+chapter/4).coerceAtMost(9));if(finale||step==3||step==7)obs+=obstacle(GardenObstacle.STONE,(2+chapter/5).coerceAtMost(7));return LazyGardenLevel(number,8,8,moves,goals.map{LazyGardenGoal(it.first,it.second)},55+(number-1)*8+when{finale->80;challenge->35;else->0},obs)}
 private val levels=authored+(111..CAMPAIGN_LEVELS).map(::generated)
 fun get(number:Int)=levels[(number-1).coerceIn(0,levels.lastIndex)]
 val count:Int get()=levels.size
}
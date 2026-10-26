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
  fun addStage(n:Int,m:Int,r:Int,a:Pair<GardenTile,Int>,b:Pair<GardenTile,Int>,c:Pair<GardenTile,Int>?=null,vararg obs:Pair<GardenObstacle,Int>){
   val rawGoals=if(c==null)listOf(a,b)else listOf(a,b,c)
   // Every objective must ask for a different tile. Earlier procedural arcs could select the same
   // tile twice, making two HUD objectives advance together and weakening the intended difficulty.
   val used=mutableSetOf<GardenTile>()
   val g=rawGoals.map{goal->
    val tile=if(goal.first !in used) goal.first else GardenTile.entries.first{it !in used}
    used+=tile
    tile to goal.second
   }
   // Campaign chapters vary the playable footprint instead of repeating one 8x8 board for 250 stages.
   // Narrow boards emphasize vertical planning; short boards emphasize horizontal planning; finales stay 8x8.
   val step=(n-1)%10
   val (rows,columns)=when(step){1,6->8 to 7;3,8->7 to 8;else->8 to 8}
   add(LazyGardenLevel(n,rows,columns,m,g.map{LazyGardenGoal(it.first,it.second)},r,obs.map{obstacle(it.first,it.second)}))
  }
  val chapters=listOf(arrayOf(GardenTile.LEAF,GardenTile.FLOWER,GardenTile.ROSE,GardenTile.BERRY,GardenTile.PEAR,GardenTile.WATER),arrayOf(GardenTile.LEAF,GardenTile.PEAR,GardenTile.FLOWER,GardenTile.WATER,GardenTile.BERRY,GardenTile.ROSE),arrayOf(GardenTile.FLOWER,GardenTile.BERRY,GardenTile.ROSE,GardenTile.LEAF,GardenTile.WATER,GardenTile.PEAR),arrayOf(GardenTile.LEAF,GardenTile.ROSE,GardenTile.BERRY,GardenTile.PEAR,GardenTile.FLOWER,GardenTile.WATER),arrayOf(GardenTile.LEAF,GardenTile.FLOWER,GardenTile.BERRY,GardenTile.ROSE,GardenTile.PEAR,GardenTile.WATER),arrayOf(GardenTile.LEAF,GardenTile.PEAR,GardenTile.FLOWER,GardenTile.BERRY,GardenTile.ROSE,GardenTile.WATER))
  var n=1;chapters.forEachIndexed{ci,t->val chapter=ci+1;repeat(10){step->val finale=step==9;val pressure=step==4||step==7||finale;val moves=when{finale->23;pressure->21+(chapter%2);else->24-(step%3)};val base=18+chapter*2+step/2;val goals=mutableListOf(t[(step+chapter)%6] to(base+4),t[(step*2+1)%6] to(base+2));if(pressure)goals+=t[(step*3+2)%6] to(12+chapter);val obs=mutableListOf<Pair<GardenObstacle,Int>>();if(n>=6)obs+=GardenObstacle.CRATE to(2+chapter+step/4);if(n>=8&&(step%2==0||pressure))obs+=GardenObstacle.IVY to(2+chapter/2+step/5);if(n>=41&&(step==1||step==4||step==7||finale))obs+=GardenObstacle.STONE to(2+(chapter-4).coerceAtLeast(0)+if(finale)1 else 0);addStage(n,moves,45+n*9,goals[0],goals[1],goals.getOrNull(2),*obs.toTypedArray());n++}}
  val tiles=GardenTile.entries
  for(num in 61..140){val chapter=(num-1)/10;val step=(num-1)%10;val finale=step==9;val pressure=step in setOf(3,4,6,7,8,9);val moves=when{finale->20;step==4||step==7->21;else->24-(step%3)};val base=26+chapter+step/2;val a=tiles[(num+1)%6] to(base+3);val b=tiles[(num*2+3)%6] to(base);val c=if(pressure)tiles[(num*3+2)%6] to(13+chapter/2+step/3) else null;val obs=mutableListOf<Pair<GardenObstacle,Int>>(GardenObstacle.CRATE to(6+chapter/3+step/4));if(step%2==0||pressure)obs+=GardenObstacle.IVY to(4+chapter/4+step/5);if(step in setOf(0,1,3,4,6,7,8,9))obs+=GardenObstacle.STONE to(3+chapter/5+if(finale)2 else 0);addStage(num,moves,55+num*20+(if(finale)60 else 0),a,b,c,*obs.toTypedArray())}
  // Chapters 15-25 are authored as deliberate ten-stage arcs rather than fallback-generated levels.
  // Each chapter opens with recovery/readability, introduces mixed blockers, peaks at stages 5/8,
  // and ends with a three-goal finale. Targets and move budgets rise in controlled bands.
  for(chapter in 15..25){
   val start=(chapter-1)*10+1
   val palette=GardenTile.entries.toList().let{p->List(6){p[(it+chapter)%6]}}
   repeat(10){step->
    val num=start+step
    val finale=step==9
    val hard=step==4||step==7
    val threeGoals=hard||finale||step==6
    val tier=chapter-15
    val moves=when{finale->19-(tier/6);hard->20-(tier/8);step==0||step==5->23-(tier/8);else->22-(tier/9)}.coerceAtLeast(17)
    val primary=palette[(step+chapter)%6] to(34+tier+step/2)
    val secondary=palette[(step*2+1)%6] to(30+tier+step/3)
    val tertiary=if(threeGoals) palette[(step*3+2)%6] to(17+tier/2+step/2) else null
    val obs=mutableListOf<Pair<GardenObstacle,Int>>()
    obs+=GardenObstacle.CRATE to(8+(tier/2)+step/4).coerceAtMost(14)
    if(step!=0||tier>=3)obs+=GardenObstacle.IVY to(6+(tier/3)+if(hard||finale)2 else step/4).coerceAtMost(12)
    if(step in setOf(2,3,4,6,7,8,9))obs+=GardenObstacle.STONE to(4+(tier/4)+if(finale)2 else if(hard)1 else 0).coerceAtMost(10)
    addStage(num,moves,3000+(num-140)*24+(if(hard)45 else 0)+(if(finale)110 else 0),primary,secondary,tertiary,*obs.toTypedArray())
   }
  }
 }
 private val levels=authored
 init {
  require(levels.size==CAMPAIGN_LEVELS){"LazyGarden campaign must contain exactly $CAMPAIGN_LEVELS authored levels, found ${levels.size}"}
  require(levels.map{it.number}==(1..CAMPAIGN_LEVELS).toList()){"LazyGarden campaign numbering must be contiguous 1..$CAMPAIGN_LEVELS"}
  require(levels.all{level->level.goals.map{it.tile}.distinct().size==level.goals.size}){"LazyGarden levels must use distinct tile objectives"}
 }
 fun get(number:Int)=levels[(number-1).coerceIn(0,levels.lastIndex)]
 val count:Int get()=levels.size
}

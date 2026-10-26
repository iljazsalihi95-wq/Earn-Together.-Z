package com.weearntogether.lazygarden

import kotlin.math.abs
import kotlin.random.Random

data class Cell(val row:Int,val col:Int)
data class MatchResult(val cells:Set<Cell>,val tiles:Map<GardenTile,Int>,val obstacles:Map<GardenObstacle,Int> = emptyMap())

enum class GardenPowerUp { HAMMER, ROW_SWEEP, COLUMN_SWEEP, CROSS_BLAST }

class LazyGardenEngine(val level:LazyGardenLevel,private val random:Random=Random(level.number.toLong()*104729L+0x57455447L)){
    val board:Array<Array<GardenTile>> = Array(level.rows){Array(level.columns){GardenTile.LEAF}}
    val obstacles:Array<Array<GardenObstacle?>> = Array(level.rows){arrayOfNulls(level.columns)}
    private val obstacleHits:Array<IntArray> = Array(level.rows){IntArray(level.columns)}
    private val activeTiles:List<GardenTile> = buildActiveTilePool()
    var movesLeft:Int=level.moves;private set
    init{fillPlayableBoard();seedObstacles()}

    /**
     * Campaign colour pressure grows with the player. Early levels use four colours,
     * mid-campaign five and advanced chapters all six. Every objective colour is always
     * included, so a generated level can never ask the player to collect an unavailable tile.
     */
    private fun buildActiveTilePool():List<GardenTile>{
        val desired=when{level.number<=20->4;level.number<=60->5;else->GardenTile.entries.size}
        val pool=linkedSetOf<GardenTile>()
        level.goals.forEach{pool+=it.tile}
        val rotation=(level.number/10)%GardenTile.entries.size
        repeat(GardenTile.entries.size){pool+=GardenTile.entries[(it+rotation)%GardenTile.entries.size]}
        return pool.take(maxOf(desired,level.goals.map{it.tile}.distinct().size))
    }
    private fun randomTile():GardenTile=activeTiles[random.nextInt(activeTiles.size)]

    fun restoreMoves(savedMoves:Int){movesLeft=savedMoves.coerceIn(0,level.moves)}
    fun shuffleBoard():Boolean{if(movesLeft<=0)return false;val old=encodeBoard();var attempts=0;do{fillBoardWithoutImmediateMatches();attempts++}while((!hasValidMove()||encodeBoard()==old)&&attempts<MAX_RESHUFFLE_ATTEMPTS);if(!hasValidMove())seedGuaranteedMove();return hasValidMove()}
    fun obstacleAt(cell:Cell):GardenObstacle?=if(inside(cell))obstacles[cell.row][cell.col]else null
    fun obstacleHitsLeft(cell:Cell):Int=if(inside(cell))obstacleHits[cell.row][cell.col]else 0
    private fun blocksSwap(cell:Cell):Boolean=when(obstacleAt(cell)){GardenObstacle.CRATE,GardenObstacle.STONE->true;else->false}
    fun encodeBoard():String=board.joinToString(""){row->row.joinToString(""){it.ordinal.toString()}}
    fun encodeObstacles():String=obstacles.joinToString(""){row->row.joinToString(""){cell->when(cell){null->"0";GardenObstacle.CRATE->"1";GardenObstacle.IVY->"2";GardenObstacle.STONE->"3"}}}
    fun encodeObstacleHits():String=obstacleHits.joinToString(","){row->row.joinToString(".")}
    fun restoreState(boardState:String,obstacleState:String,obstacleHitState:String?=null):Boolean{
        val cells=level.rows*level.columns;if(boardState.length!=cells||obstacleState.length!=cells)return false
        val savedHits=obstacleHitState?.split(',')?.takeIf{it.size==level.rows}?.map{row->row.split('.').mapNotNull(String::toIntOrNull).takeIf{it.size==level.columns}?:return@map emptyList()}
        val validSavedHits=savedHits?.takeIf{rows->rows.size==level.rows&&rows.all{it.size==level.columns}}
        var i=0
        for(r in 0 until level.rows)for(c in 0 until level.columns){val tile=boardState[i].digitToIntOrNull()?.let{GardenTile.entries.getOrNull(it)}?:return false;val obstacle=when(obstacleState[i]){'0'->null;'1'->GardenObstacle.CRATE;'2'->GardenObstacle.IVY;'3'->GardenObstacle.STONE;else->return false};board[r][c]=tile;obstacles[r][c]=obstacle;val max=durability(obstacle);obstacleHits[r][c]=if(obstacle==null)0 else validSavedHits?.get(r)?.get(c)?.coerceIn(1,max)?:max;i++}
        if(findMatches().cells.isNotEmpty()||!hasValidMove())return false;return true
    }

    fun swap(a:Cell,b:Cell):MatchResult?{
        if(movesLeft<=0||!inside(a)||!inside(b)||abs(a.row-b.row)+abs(a.col-b.col)!=1)return null;if(blocksSwap(a)||blocksSwap(b))return null
        swapTiles(a,b);var matches=findMatches();if(matches.cells.isEmpty()){swapTiles(a,b);return null};movesLeft--
        val collected=mutableMapOf<GardenTile,Int>();val cleared=mutableMapOf<GardenObstacle,Int>();val resolvedCells=linkedSetOf<Cell>()
        while(matches.cells.isNotEmpty()){
            val powered=expandPowerClear(matches);resolvedCells+=powered.cells;powered.tiles.forEach{(t,n)->collected[t]=(collected[t]?:0)+n}
            damageObstacles(powered.cells).forEach{(o,n)->cleared[o]=(cleared[o]?:0)+n};collapseAndRefill(powered.cells.filterNot(::blocksSwap).toSet());matches=findMatches()
        }
        ensurePlayableBoard();return MatchResult(resolvedCells,collected,cleared)
    }

    fun activatePowerUp(powerUp:GardenPowerUp,target:Cell):MatchResult?{
        if(!inside(target)||movesLeft<=0)return null
        val affected=linkedSetOf<Cell>()
        when(powerUp){
            GardenPowerUp.HAMMER->affected+=target
            GardenPowerUp.ROW_SWEEP->for(c in 0 until level.columns)affected+=Cell(target.row,c)
            GardenPowerUp.COLUMN_SWEEP->for(r in 0 until level.rows)affected+=Cell(r,target.col)
            GardenPowerUp.CROSS_BLAST->{for(c in 0 until level.columns)affected+=Cell(target.row,c);for(r in 0 until level.rows)affected+=Cell(r,target.col)}
        }
        val tiles=affected.filterNot(::blocksSwap).groupingBy{board[it.row][it.col]}.eachCount()
        val cleared=damageObstacles(affected)
        collapseAndRefill(affected.filterNot(::blocksSwap).toSet())
        var cascade=findMatches();val allCells=linkedSetOf<Cell>().apply{addAll(affected)};val allTiles=tiles.toMutableMap();val allObstacles=cleared.toMutableMap()
        while(cascade.cells.isNotEmpty()){
            val powered=expandPowerClear(cascade);allCells.addAll(powered.cells);powered.tiles.forEach{(t,n)->allTiles[t]=(allTiles[t]?:0)+n};damageObstacles(powered.cells).forEach{(o,n)->allObstacles[o]=(allObstacles[o]?:0)+n};collapseAndRefill(powered.cells.filterNot(::blocksSwap).toSet());cascade=findMatches()
        }
        ensurePlayableBoard();return MatchResult(allCells,allTiles,allObstacles)
    }

    /**
     * Premium match shapes have distinct board consequences:
     * - 4 in a line sweeps that full row/column.
     * - 5+ in a line adds the perpendicular sweep through the match centre.
     * - T/L intersections (3+ horizontal AND 3+ vertical through one tile) create a 3x3 area blast.
     * The shape is resolved directly from the matched cells, so cascades can trigger the same mechanics.
     */
    private fun expandPowerClear(match:MatchResult):MatchResult{
        if(match.cells.size<4)return match
        val expanded=linkedSetOf<Cell>().apply{addAll(match.cells)}
        val rows=match.cells.groupBy{it.row};val columns=match.cells.groupBy{it.col}
        rows.filterValues{it.size>=4}.forEach{(row,line)->for(c in 0 until level.columns)expanded+=Cell(row,c);if(line.size>=5){val pivot=line[line.size/2];for(r in 0 until level.rows)expanded+=Cell(r,pivot.col)}}
        columns.filterValues{it.size>=4}.forEach{(col,line)->for(r in 0 until level.rows)expanded+=Cell(r,col);if(line.size>=5){val pivot=line[line.size/2];for(c in 0 until level.columns)expanded+=Cell(pivot.row,c)}}
        val intersections=match.cells.filter{cell->(rows[cell.row]?.size?:0)>=3&&(columns[cell.col]?.size?:0)>=3}
        intersections.forEach{pivot->for(dr in -1..1)for(dc in -1..1){val cell=Cell(pivot.row+dr,pivot.col+dc);if(inside(cell))expanded+=cell}}
        return MatchResult(expanded,expanded.filterNot(::blocksSwap).groupingBy{board[it.row][it.col]}.eachCount())
    }

    fun findMatches():MatchResult{
        val cells=linkedSetOf<Cell>()
        fun blocked(r:Int,c:Int):Boolean { return obstacles[r][c]==GardenObstacle.STONE || obstacles[r][c]==GardenObstacle.CRATE }
        for(r in board.indices){var start=0;while(start<level.columns){if(blocked(r,start)){start++;continue};var end=start+1;while(end<level.columns&&!blocked(r,end)&&board[r][end]==board[r][start])end++;if(end-start>=3)for(c in start until end)cells+=Cell(r,c);start=end}}
        for(c in 0 until level.columns){var start=0;while(start<level.rows){if(blocked(start,c)){start++;continue};var end=start+1;while(end<level.rows&&!blocked(end,c)&&board[end][c]==board[start][c])end++;if(end-start>=3)for(r in start until end)cells+=Cell(r,c);start=end}}
        return MatchResult(cells,cells.groupingBy{board[it.row][it.col]}.eachCount())
    }
    fun findHint():Pair<Cell,Cell>?{if(movesLeft<=0)return null;for(r in 0 until level.rows)for(c in 0 until level.columns){val a=Cell(r,c);if(blocksSwap(a))continue;if(c+1<level.columns){val b=Cell(r,c+1);if(!blocksSwap(b)&&createsMatch(a,b))return a to b};if(r+1<level.rows){val b=Cell(r+1,c);if(!blocksSwap(b)&&createsMatch(a,b))return a to b}};return null}
    fun hasValidMove():Boolean=findHint()!=null
    private fun durability(obstacle:GardenObstacle?):Int=when(obstacle){GardenObstacle.IVY->1;GardenObstacle.CRATE->if(level.number>=80)2 else 1;GardenObstacle.STONE->if(level.number>=150)3 else 2;null->0}
    private fun seedObstacles(){val reserved=buildSet{for(r in 0..1)for(c in 0..2)add(Cell(r,c))};val candidates=mutableListOf<Cell>();for(r in 0 until level.rows)for(c in 0 until level.columns){val cell=Cell(r,c);if(cell !in reserved)candidates+=cell};val centerR=(level.rows-1)/2.0;val centerC=(level.columns-1)/2.0;val pattern=level.number%5;val ordered=when(pattern){0->candidates.sortedWith(compareBy<Cell>({abs(it.row-centerR)+abs(it.col-centerC)},{it.row},{it.col}));1->candidates.sortedWith(compareBy<Cell>({abs(it.row-it.col)},{it.row+it.col},{it.row}));2->candidates.sortedWith(compareBy<Cell>({abs((level.columns-1-it.col)-it.row)},{it.row+it.col},{it.col}));3->candidates.sortedWith(compareBy<Cell>({if((it.row+it.col)%2==0)0 else 1},{abs(it.row-centerR)+abs(it.col-centerC)},{it.row},{it.col}));else->candidates.sortedWith(compareBy<Cell>({minOf(it.row,it.col,level.rows-1-it.row,level.columns-1-it.col)},{it.row},{it.col}))}.toMutableList();val requested=level.obstacleGoals.sumOf{it.target.coerceAtLeast(0)};require(requested<=ordered.size){"Level ${level.number} requests $requested obstacles but only ${ordered.size} cells are available"};var cursor=0;level.obstacleGoals.forEach{goal->repeat(goal.target.coerceAtLeast(0)){val x=ordered[cursor++];obstacles[x.row][x.col]=goal.obstacle;obstacleHits[x.row][x.col]=durability(goal.obstacle)};if(cursor<ordered.size)ordered.subList(cursor,ordered.size).shuffle(random)};ensurePlayableBoard()}
    private fun damageObstacles(matched:Set<Cell>):Map<GardenObstacle,Int>{val cleared=mutableMapOf<GardenObstacle,Int>();val targets=linkedSetOf<Cell>();matched.filter(::inside).forEach{x->if(obstacles[x.row][x.col]!=null)targets+=x;targets+=Cell(x.row-1,x.col);targets+=Cell(x.row+1,x.col);targets+=Cell(x.row,x.col-1);targets+=Cell(x.row,x.col+1)};targets.filter(::inside).forEach{x->val o=obstacles[x.row][x.col]?:return@forEach;obstacleHits[x.row][x.col]=(obstacleHits[x.row][x.col]-1).coerceAtLeast(0);if(obstacleHits[x.row][x.col]==0){obstacles[x.row][x.col]=null;cleared[o]=(cleared[o]?:0)+1}};return cleared}
    private fun createsMatch(a:Cell,b:Cell):Boolean{swapTiles(a,b);val v=findMatches().cells.isNotEmpty();swapTiles(a,b);return v}
    private fun swapTiles(a:Cell,b:Cell){val x=board[a.row][a.col];board[a.row][a.col]=board[b.row][b.col];board[b.row][b.col]=x}
    private fun fillPlayableBoard(){var attempts=0;do{fillBoardWithoutImmediateMatches();attempts++}while(!hasValidMove()&&attempts<MAX_RESHUFFLE_ATTEMPTS);if(!hasValidMove())seedGuaranteedMove()}
    private fun fillBoardWithoutImmediateMatches(){for(r in board.indices)for(c in board[r].indices){do{board[r][c]=randomTile()}while((c>=2&&board[r][c]==board[r][c-1]&&board[r][c]==board[r][c-2])||(r>=2&&board[r][c]==board[r-1][c]&&board[r][c]==board[r-2][c]))}}
    private fun ensurePlayableBoard(){if(hasValidMove())return;var attempts=0;do{fillBoardWithoutImmediateMatches();attempts++}while(!hasValidMove()&&attempts<MAX_RESHUFFLE_ATTEMPTS);if(!hasValidMove())seedGuaranteedMove()}
    private fun seedGuaranteedMove(){if(level.rows<2||level.columns<3)return;for(r in 0..1)for(c in 0..2){obstacles[r][c]=null;obstacleHits[r][c]=0};val a=activeTiles[0];val b=activeTiles.getOrElse(1){a};val c=activeTiles.getOrElse(2){a};board[0][0]=a;board[0][1]=b;board[0][2]=a;board[1][0]=c;board[1][1]=a;board[1][2]=b}
    private fun collapseAndRefill(matched:Set<Cell>){for(c in 0 until level.columns){var segmentBottom=level.rows-1;while(segmentBottom>=0){if(blocksSwap(Cell(segmentBottom,c))){segmentBottom--;continue};var segmentTop=segmentBottom;while(segmentTop-1>=0&&!blocksSwap(Cell(segmentTop-1,c)))segmentTop--;val survivors=(segmentBottom downTo segmentTop).filter{Cell(it,c)!in matched}.map{board[it][c]};var survivorIndex=0;for(r in segmentBottom downTo segmentTop)board[r][c]=if(survivorIndex<survivors.size)survivors[survivorIndex++] else randomTile();segmentBottom=segmentTop-1}}}
    private fun inside(cell:Cell)=cell.row in 0 until level.rows&&cell.col in 0 until level.columns
    companion object{private const val MAX_RESHUFFLE_ATTEMPTS=40}
}
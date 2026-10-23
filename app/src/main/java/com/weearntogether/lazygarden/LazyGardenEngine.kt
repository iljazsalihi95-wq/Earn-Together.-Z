package com.weearntogether.lazygarden

import kotlin.math.abs
import kotlin.random.Random

data class Cell(val row:Int,val col:Int)
enum class GardenPowerUp { ROW_BLAST, COLUMN_BLAST }
data class MatchResult(val cells:Set<Cell>,val tiles:Map<GardenTile,Int>,val obstacles:Map<GardenObstacle,Int> = emptyMap(),val powerUpsCreated:Int = 0)

class LazyGardenEngine(val level:LazyGardenLevel,private val random:Random=Random(level.number.toLong()*104729L+0x57455447L)){
    val board:Array<Array<GardenTile>> = Array(level.rows){Array(level.columns){GardenTile.LEAF}}
    val obstacles:Array<Array<GardenObstacle?>> = Array(level.rows){arrayOfNulls(level.columns)}
    val powerUps:Array<Array<GardenPowerUp?>> = Array(level.rows){arrayOfNulls(level.columns)}
    var movesLeft:Int=level.moves;private set
    init{fillPlayableBoard();seedObstacles()}

    fun restoreMoves(savedMoves:Int){movesLeft=savedMoves.coerceIn(0,level.moves)}
    fun shuffleBoard():Boolean{if(movesLeft<=0)return false;val old=encodeBoard();var attempts=0;do{fillBoardWithoutImmediateMatches();clearPowerUps();attempts++}while((!hasValidMove()||encodeBoard()==old)&&attempts<MAX_RESHUFFLE_ATTEMPTS);if(!hasValidMove())seedGuaranteedMove();return hasValidMove()}
    fun obstacleAt(cell:Cell):GardenObstacle?=if(inside(cell))obstacles[cell.row][cell.col]else null
    fun powerUpAt(cell:Cell):GardenPowerUp?=if(inside(cell))powerUps[cell.row][cell.col]else null
    private fun blocksSwap(cell:Cell):Boolean=when(obstacleAt(cell)){GardenObstacle.CRATE,GardenObstacle.STONE->true;else->false}
    fun encodeBoard():String=board.joinToString(""){row->row.joinToString(""){it.ordinal.toString()}}
    fun encodeObstacles():String=obstacles.joinToString(""){row->row.joinToString(""){cell->when(cell){null->"0";GardenObstacle.CRATE->"1";GardenObstacle.IVY->"2";GardenObstacle.STONE->"3"}}}
    fun restoreState(boardState:String,obstacleState:String):Boolean{val cells=level.rows*level.columns;if(boardState.length!=cells||obstacleState.length!=cells)return false;var i=0;for(r in 0 until level.rows)for(c in 0 until level.columns){val tile=boardState[i].digitToIntOrNull()?.let{GardenTile.entries.getOrNull(it)}?:return false;val obstacle=when(obstacleState[i]){'0'->null;'1'->GardenObstacle.CRATE;'2'->GardenObstacle.IVY;'3'->GardenObstacle.STONE;else->return false};board[r][c]=tile;obstacles[r][c]=obstacle;i++};clearPowerUps();if(findMatches().cells.isNotEmpty()||!hasValidMove())return false;return true}

    fun swap(a:Cell,b:Cell):MatchResult?{
        if(movesLeft<=0||!inside(a)||!inside(b)||abs(a.row-b.row)+abs(a.col-b.col)!=1)return null
        if(blocksSwap(a)||blocksSwap(b))return null
        val armedA=powerUps[a.row][a.col];val armedB=powerUps[b.row][b.col]
        swapTiles(a,b);swapPowerUps(a,b)
        var matches=findMatches()
        if(matches.cells.isEmpty()&&armedA==null&&armedB==null){swapTiles(a,b);swapPowerUps(a,b);return null}
        movesLeft--
        val collected=mutableMapOf<GardenTile,Int>();val cleared=mutableMapOf<GardenObstacle,Int>();var created=0
        var forced=linkedSetOf<Cell>()
        powerUps[b.row][b.col]?.let{forced+=blastCells(b,it);powerUps[b.row][b.col]=null}
        powerUps[a.row][a.col]?.let{forced+=blastCells(a,it);powerUps[a.row][a.col]=null}
        while(matches.cells.isNotEmpty()||forced.isNotEmpty()){
            val clear=linkedSetOf<Cell>().apply{addAll(matches.cells);addAll(forced)}
            clear.filter(::inside).forEach{x->collected[board[x.row][x.col]]=(collected[board[x.row][x.col]]?:0)+1}
            clearObstaclesForMatch(clear).forEach{(o,n)->cleared[o]=(cleared[o]?:0)+n}
            val spawn=if(forced.isEmpty())powerUpFor(matches.cells,a,b)else null
            clear.forEach{x->if(inside(x))powerUps[x.row][x.col]=null}
            collapseAndRefill(clear)
            if(spawn!=null){val (cell,power)=spawn;powerUps[cell.row][cell.col]=power;created++}
            forced=linkedSetOf();matches=findMatches()
        }
        ensurePlayableBoard();return MatchResult(emptySet(),collected,cleared,created)
    }

    fun findMatches():MatchResult{val cells=linkedSetOf<Cell>();for(r in board.indices){var start=0;while(start<level.columns){var end=start+1;while(end<level.columns&&board[r][end]==board[r][start])end++;if(end-start>=3)for(c in start until end)if(obstacles[r][c]!=GardenObstacle.STONE&&obstacles[r][c]!=GardenObstacle.CRATE)cells+=Cell(r,c);start=end}};for(c in 0 until level.columns){var start=0;while(start<level.rows){var end=start+1;while(end<level.rows&&board[end][c]==board[start][c])end++;if(end-start>=3)for(r in start until end)if(obstacles[r][c]!=GardenObstacle.STONE&&obstacles[r][c]!=GardenObstacle.CRATE)cells+=Cell(r,c);start=end}};return MatchResult(cells,cells.groupingBy{board[it.row][it.col]}.eachCount())}
    fun findHint():Pair<Cell,Cell>?{if(movesLeft<=0)return null;for(r in 0 until level.rows)for(c in 0 until level.columns){val a=Cell(r,c);if(blocksSwap(a))continue;if(c+1<level.columns){val b=Cell(r,c+1);if(!blocksSwap(b)&&(powerUpAt(a)!=null||powerUpAt(b)!=null||createsMatch(a,b)))return a to b};if(r+1<level.rows){val b=Cell(r+1,c);if(!blocksSwap(b)&&(powerUpAt(a)!=null||powerUpAt(b)!=null||createsMatch(a,b)))return a to b}};return null}
    fun hasValidMove():Boolean=findHint()!=null
    private fun powerUpFor(cells:Set<Cell>,a:Cell,b:Cell):Pair<Cell,GardenPowerUp>?{if(cells.size<4)return null;val anchor=when{b in cells->b;a in cells->a;else->cells.first()};val sameRow=cells.count{it.row==anchor.row}>=4;val sameCol=cells.count{it.col==anchor.col}>=4;return when{sameRow->anchor to GardenPowerUp.ROW_BLAST;sameCol->anchor to GardenPowerUp.COLUMN_BLAST;else->null}}
    private fun blastCells(cell:Cell,power:GardenPowerUp):Set<Cell>=when(power){GardenPowerUp.ROW_BLAST->(0 until level.columns).map{Cell(cell.row,it)}.toSet();GardenPowerUp.COLUMN_BLAST->(0 until level.rows).map{Cell(it,cell.col)}.toSet()}
    private fun swapPowerUps(a:Cell,b:Cell){val x=powerUps[a.row][a.col];powerUps[a.row][a.col]=powerUps[b.row][b.col];powerUps[b.row][b.col]=x}
    private fun clearPowerUps(){for(r in powerUps.indices)for(c in powerUps[r].indices)powerUps[r][c]=null}
    private fun seedObstacles(){val candidates=mutableListOf<Cell>();for(r in 1 until level.rows-1)for(c in 1 until level.columns-1)candidates+=Cell(r,c);candidates.shuffle(random);var cursor=0;level.obstacleGoals.forEach{goal->repeat(goal.target.coerceAtMost(candidates.size-cursor)){val x=candidates[cursor++];obstacles[x.row][x.col]=goal.obstacle}};for(r in 0..1)for(c in 0..2)obstacles[r][c]=null;ensurePlayableBoard()}
    private fun clearObstaclesForMatch(matched:Set<Cell>):Map<GardenObstacle,Int>{val cleared=mutableMapOf<GardenObstacle,Int>();val adjacent=linkedSetOf<Cell>();matched.forEach{x->adjacent+=Cell(x.row-1,x.col);adjacent+=Cell(x.row+1,x.col);adjacent+=Cell(x.row,x.col-1);adjacent+=Cell(x.row,x.col+1)};matched.filter(::inside).forEach{x->if(obstacles[x.row][x.col]==GardenObstacle.IVY){obstacles[x.row][x.col]=null;cleared[GardenObstacle.IVY]=(cleared[GardenObstacle.IVY]?:0)+1}};adjacent.filter(::inside).forEach{x->val o=obstacles[x.row][x.col]?:return@forEach;if(o==GardenObstacle.CRATE||o==GardenObstacle.STONE){obstacles[x.row][x.col]=null;cleared[o]=(cleared[o]?:0)+1}};return cleared}
    private fun createsMatch(a:Cell,b:Cell):Boolean{swapTiles(a,b);val v=findMatches().cells.isNotEmpty();swapTiles(a,b);return v}
    private fun swapTiles(a:Cell,b:Cell){val x=board[a.row][a.col];board[a.row][a.col]=board[b.row][b.col];board[b.row][b.col]=x}
    private fun fillPlayableBoard(){var attempts=0;do{fillBoardWithoutImmediateMatches();attempts++}while(!hasValidMove()&&attempts<MAX_RESHUFFLE_ATTEMPTS);if(!hasValidMove())seedGuaranteedMove()}
    private fun fillBoardWithoutImmediateMatches(){for(r in board.indices)for(c in board[r].indices){do{board[r][c]=GardenTile.entries[random.nextInt(GardenTile.entries.size)]}while((c>=2&&board[r][c]==board[r][c-1]&&board[r][c]==board[r][c-2])||(r>=2&&board[r][c]==board[r-1][c]&&board[r][c]==board[r-2][c]))}}
    private fun ensurePlayableBoard(){if(hasValidMove())return;var attempts=0;do{fillBoardWithoutImmediateMatches();clearPowerUps();attempts++}while(!hasValidMove()&&attempts<MAX_RESHUFFLE_ATTEMPTS);if(!hasValidMove())seedGuaranteedMove()}
    private fun seedGuaranteedMove(){if(level.rows<2||level.columns<3)return;for(r in 0..1)for(c in 0..2)obstacles[r][c]=null;board[0][0]=GardenTile.LEAF;board[0][1]=GardenTile.FLOWER;board[0][2]=GardenTile.LEAF;board[1][0]=GardenTile.ROSE;board[1][1]=GardenTile.LEAF;board[1][2]=GardenTile.BERRY}
    private fun collapseAndRefill(matched:Set<Cell>){for(c in 0 until level.columns){val survivorRows=(level.rows-1 downTo 0).filter{Cell(it,c)!in matched};val survivorTiles=survivorRows.map{board[it][c]};val survivorPowers=survivorRows.map{powerUps[it][c]};var i=0;for(r in level.rows-1 downTo 0){if(i<survivorTiles.size){board[r][c]=survivorTiles[i];powerUps[r][c]=survivorPowers[i];i++}else{board[r][c]=GardenTile.entries[random.nextInt(GardenTile.entries.size)];powerUps[r][c]=null}}}}
    private fun inside(cell:Cell)=cell.row in 0 until level.rows&&cell.col in 0 until level.columns
    companion object{private const val MAX_RESHUFFLE_ATTEMPTS=40}
}

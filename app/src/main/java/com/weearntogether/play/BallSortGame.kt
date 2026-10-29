package com.weearntogether.play

import kotlin.random.Random

/** Pure sorting rules. Generation records a reversible sequence from a solved board.
 * A legal move pours the whole exposed run of one colour (up to destination capacity),
 * matching premium ball-sort behaviour instead of moving a single ball at a time.
 */
class BallSortGame(val level:Int, saved:List<List<Int>>?=null){
    val capacity=4
    val colors=(3+(level-1)/3).coerceAtMost(9)
    val emptyTubes=when { level<12->2; level<45->2; else->3 }
    var tubes:MutableList<MutableList<Int>> = saved?.map { it.toMutableList() }?.toMutableList()
        ?: MutableList(colors+emptyTubes){i->if(i<colors)MutableList(capacity){i} else mutableListOf()}
        private set
    val history=mutableListOf<List<List<Int>>>()
    var moves=0
    var lastPourCount=0
        private set

    init{if(saved==null)scramble()}

    fun won()=tubes.all { it.isEmpty() || (it.size==capacity && it.distinct().size==1) }

    private fun exposedRun(from:Int):Int{
        if(from !in tubes.indices || tubes[from].isEmpty())return 0
        val src=tubes[from]; val color=src.last(); var count=0
        for(i in src.lastIndex downTo 0){if(src[i]!=color)break;count++}
        return count
    }

    fun canMove(from:Int,to:Int)=from in tubes.indices&&to in tubes.indices&&from!=to&&
        tubes[from].isNotEmpty()&&tubes[to].size<capacity&&
        (tubes[to].isEmpty()||tubes[to].last()==tubes[from].last())

    fun move(from:Int,to:Int):Boolean{
        if(!canMove(from,to))return false
        history.add(tubes.map { it.toList() })
        val count=minOf(exposedRun(from),capacity-tubes[to].size)
        repeat(count){tubes[to].add(tubes[from].removeAt(tubes[from].lastIndex))}
        lastPourCount=count
        moves++
        return true
    }

    fun undo():Boolean{
        if(history.isEmpty())return false
        tubes=history.removeAt(history.lastIndex).map{it.toMutableList()}.toMutableList()
        moves=(moves-1).coerceAtLeast(0);lastPourCount=0
        return true
    }

    private fun scramble(){
        val random=Random(level*7919)
        // Higher levels get longer reversible mixing sequences while remaining solvable.
        val rounds=140+level.coerceAtMost(120)*5
        repeat(rounds){
            val pairs=mutableListOf<Pair<Int,Int>>()
            for(a in tubes.indices)for(b in tubes.indices){
                val src=tubes[a];val dst=tubes[b]
                if(a==b||src.isEmpty()||dst.size==capacity)continue
                // Reverse construction: only split a uniform exposed run, preserving a path home.
                if(src.size>1&&src[src.lastIndex-1]!=src.last())continue
                pairs+=a to b
            }
            if(pairs.isNotEmpty()){
                val(a,b)=pairs[random.nextInt(pairs.size)]
                tubes[b].add(tubes[a].removeAt(tubes[a].lastIndex))
            }
        }
        if(won()){
            val a=tubes.indexOfFirst{it.isNotEmpty()};val b=tubes.indexOfFirst{it.isEmpty()}
            if(a>=0&&b>=0)tubes[b].add(tubes[a].removeAt(tubes[a].lastIndex))
        }
    }
}

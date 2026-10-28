package com.weearntogether.play

import kotlin.random.Random

/** Pure sorting rules. Generation records a reversible sequence from a solved board. */
class BallSortGame(val level:Int, saved:List<List<Int>>?=null){
    val colors=(3+(level-1)/4).coerceAtMost(8)
    var tubes:MutableList<MutableList<Int>> = saved?.map { it.toMutableList() }?.toMutableList() ?: MutableList(colors+2){i->if(i<colors)MutableList(4){i} else mutableListOf()}
        private set
    val history=mutableListOf<List<List<Int>>>()
    var moves=0
    init{if(saved==null)scramble()}
    fun won()=tubes.all { it.isEmpty() || (it.size==4 && it.distinct().size==1) }
    fun canMove(from:Int,to:Int)=from in tubes.indices&&to in tubes.indices&&from!=to&&tubes[from].isNotEmpty()&&tubes[to].size<4&&(tubes[to].isEmpty()||tubes[to].last()==tubes[from].last())
    fun move(from:Int,to:Int):Boolean{if(!canMove(from,to))return false;history.add(tubes.map { it.toList() });tubes[to].add(tubes[from].removeAt(tubes[from].lastIndex));moves++;return true}
    fun undo():Boolean{if(history.isEmpty())return false;tubes=history.removeAt(history.lastIndex).map{it.toMutableList()}.toMutableList();moves=(moves-1).coerceAtLeast(0);return true}
    private fun scramble(){val random=Random(level*7919);repeat(100+level*3){
        val pairs=mutableListOf<Pair<Int,Int>>()
        for(a in tubes.indices)for(b in tubes.indices){val src=tubes[a];val dst=tubes[b];if(a==b||src.isEmpty()||dst.size==4)continue
            // Reverse of a legal single-ball pour: the source must expose the same color or empty.
            if(src.size>1&&src[src.lastIndex-1]!=src.last())continue
            pairs+=a to b
        }
        if(pairs.isNotEmpty()){val(a,b)=pairs[random.nextInt(pairs.size)];tubes[b].add(tubes[a].removeAt(tubes[a].lastIndex))}
    };if(won()){val a=tubes.indexOfFirst{it.isNotEmpty()};val b=tubes.indexOfFirst{it.isEmpty()};if(a>=0&&b>=0)tubes[b].add(tubes[a].removeAt(tubes[a].lastIndex))}}
}

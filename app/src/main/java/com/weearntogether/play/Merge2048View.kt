package com.weearntogether.play

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/** Native swipe-based 2048 game with persistent score and board. */
class Merge2048View(context: Context): View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs=context.getSharedPreferences("merge2048",Context.MODE_PRIVATE)
    private val board=Array(4){IntArray(4)}
    private var score=0
    private var best=prefs.getInt("best",0)
    private var downX=0f
    private var downY=0f
    private var won=false
    private var lost=false
    init {
        val saved=prefs.getString("board",null)?.split(",")?.mapNotNull{it.toIntOrNull()}
        if(saved?.size==16 && saved.all{it>=0 && it<=131072}) {
            for(i in 0..15)board[i/4][i%4]=saved[i]
            score=prefs.getInt("score",0)
            won=board.any{row->row.any{it>=2048}}
            lost=!canMove()
        } else restart()
    }
    fun restart(){
        board.forEach{it.fill(0)}
        score=0;won=false;lost=false
        spawn();spawn();save();invalidate()
    }
    private fun spawn(){
        val free=(0..15).filter{board[it/4][it%4]==0}
        if(free.isEmpty())return
        val index=free.random()
        board[index/4][index%4]=if(Random.nextInt(10)==0)4 else 2
    }
    private fun save(){
        prefs.edit().putString("board",board.flatMap{it.asList()}.joinToString(","))
            .putInt("score",score).putInt("best",best).apply()
    }
    private fun canMove():Boolean {
        for(r in 0..3)for(c in 0..3){
            if(board[r][c]==0)return true
            if(r<3 && board[r][c]==board[r+1][c])return true
            if(c<3 && board[r][c]==board[r][c+1])return true
        }
        return false
    }
    private fun compress(line:IntArray):IntArray{
        val nums=line.filter{it!=0}
        val out=mutableListOf<Int>()
        var i=0
        while(i<nums.size){
            if(i+1<nums.size && nums[i]==nums[i+1]){
                val value=nums[i]*2
                out.add(value);score+=value;i+=2
            }else{out.add(nums[i]);i++}
        }
        while(out.size<4)out.add(0)
        return out.toIntArray()
    }
    private fun move(direction:Int){
        if(lost)return
        val old=board.flatMap{it.asList()}
        for(i in 0..3){
            val line=IntArray(4){j->when(direction){
                0->board[i][j]
                1->board[i][3-j]
                2->board[j][i]
                else->board[3-j][i]
            }}
            val result=compress(line)
            for(j in 0..3)when(direction){
                0->board[i][j]=result[j]
                1->board[i][3-j]=result[j]
                2->board[j][i]=result[j]
                else->board[3-j][i]=result[j]
            }
        }
        if(old!=board.flatMap{it.asList()}){
            spawn()
            won=board.any{row->row.any{it>=2048}}
            lost=!canMove()
            if(score>best)best=score
            save();invalidate()
        }
    }
    override fun onTouchEvent(event:MotionEvent):Boolean{
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN->{downX=event.x;downY=event.y;return true}
            MotionEvent.ACTION_UP->{
                val dx=event.x-downX;val dy=event.y-downY
                if(maxOf(abs(dx),abs(dy))<35f)return true
                if(abs(dx)>abs(dy))move(if(dx<0)0 else 1)
                else move(if(dy<0)2 else 3)
                return true
            }
        }
        return true
    }
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val cell=min((width-40f)/4f,160f)
        val ox=(width-4*cell)/2f
        val oy=160f
        paint.color=Color.WHITE;paint.textSize=37f;paint.isFakeBoldText=true
        canvas.drawText("Pikë: $score",ox,57f,paint)
        paint.color=0xffffca57.toInt();paint.textSize=26f
        canvas.drawText("Rekordi: $best",ox,95f,paint)
        paint.textSize=22f;paint.color=0xffccd5ef.toInt()
        canvas.drawText("Rrëshqit lart, poshtë, majtas, djathtas",ox,132f,paint)
        val colors=mapOf(0 to 0xff243250.toInt(),2 to 0xff4063a2.toInt(),
            4 to 0xff5569b9.toInt(),8 to 0xff7555ca.toInt(),
            16 to 0xffa354cf.toInt(),32 to 0xffd85a9c.toInt(),
            64 to 0xffee7776.toInt(),128 to 0xffdaa54b.toInt(),
            256 to 0xffe8b94e.toInt(),512 to 0xfff0c45c.toInt(),
            1024 to 0xffe8c74a.toInt(),2048 to 0xffffd76d.toInt())
        for(r in 0..3)for(c in 0..3){
            val value=board[r][c]
            val x=ox+c*cell+3;val y=oy+r*cell+3
            paint.color=colors[value]?:0xffffd76d.toInt()
            canvas.drawRoundRect(x,y,x+cell-6,y+cell-6,15f,15f,paint)
            if(value!=0){
                paint.color=if(value>=1024)Color.BLACK else Color.WHITE
                paint.textSize=if(value>=1024)cell*.28f else cell*.38f
                paint.isFakeBoldText=true
                val label=value.toString()
                canvas.drawText(label,x+(cell-6-paint.measureText(label))/2,y+cell*.62f,paint)
            }
        }
        paint.color=0xffffca57.toInt();paint.textSize=26f
        if(won)canvas.drawText("2048! Vazhdo të luash!",ox,oy+4*cell+45f,paint)
        if(lost)canvas.drawText("Fund i lojës – Rifillo!",ox,oy+4*cell+45f,paint)
    }
}

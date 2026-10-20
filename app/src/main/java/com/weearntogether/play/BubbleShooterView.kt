package com.weearntogether.play

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/** Native bubble shooter: aim, shoot, snap, clear groups of 3+, drop detached bubbles. */
class BubbleShooterView(context: Context): View(context) {
    private val colors=intArrayOf(0xfffe6398.toInt(),0xff52d3fa.toInt(),0xffffca57.toInt(),0xff9c7bff.toInt())
    private val grid=Array(12){IntArray(8){-1}}
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private var next=Random.nextInt(colors.size)
    private var shots=0
    private var points=0
    private var ended=false
    private var moving=false
    private var px=0f; private var py=0f; private var vx=0f; private var vy=0f
    private var lastFrame=0L
    init { for(r in 0..3)for(c in 0..7)grid[r][c]=Random.nextInt(colors.size) }
    private fun radius()=width.toFloat()/17f
    private fun centerX(c:Int,r:Int):Float=radius()*(1+2*c+(r%2))
    private fun centerY(r:Int):Float=radius()*(1.3f+1.75f*r)
    private fun shooterY():Float=min(height.toFloat()-radius()*3, radius()*29)
    private fun neighbors(r:Int,c:Int):List<Pair<Int,Int>> {
        val d=if(r%2==0) arrayOf(-1 to -1,-1 to 0,0 to -1,0 to 1,1 to -1,1 to 0)
              else arrayOf(-1 to 0,-1 to 1,0 to -1,0 to 1,1 to 0,1 to 1)
        return d.map{(dr,dc)->r+dr to c+dc}.filter{(rr,cc)->rr in 0..11 && cc in 0..7}
    }
    private fun group(r:Int,c:Int,matching:Boolean):Set<Pair<Int,Int>> {
        val result=mutableSetOf(r to c)
        val queue=ArrayDeque<Pair<Int,Int>>();queue.add(r to c)
        while(queue.isNotEmpty()) {
            val (y,x)=queue.removeFirst()
            for((yy,xx) in neighbors(y,x)) if(grid[yy][xx]>=0 && (!matching || grid[yy][xx]==grid[r][c]) && result.add(yy to xx))queue.add(yy to xx)
        }
        return result
    }
    private fun snap() {
        moving=false
        val candidates=mutableListOf<Triple<Int,Int,Float>>()
        for(r in 0..11)for(c in 0..7)if(grid[r][c]<0) {
            val connected=r==0 || neighbors(r,c).any{(rr,cc)->grid[rr][cc]>=0}
            if(connected) candidates.add(Triple(r,c,hypot(px-centerX(c,r),py-centerY(r))))
        }
        val nearest=candidates.minByOrNull{it.third} ?: run{ended=true;invalidate();return}
        val r=nearest.first;val c=nearest.second
        grid[r][c]=next
        val matches=group(r,c,true)
        if(matches.size>=3) {
            matches.forEach{(rr,cc)->grid[rr][cc]=-1}
            points+=matches.size*10
            val attached=mutableSetOf<Pair<Int,Int>>()
            for(col in 0..7)if(grid[0][col]>=0)attached.addAll(group(0,col,false))
            for(rr in 0..11)for(cc in 0..7)if(grid[rr][cc]>=0 && (rr to cc) !in attached) {
                grid[rr][cc]=-1;points+=15
            }
        }
        shots++
        next=Random.nextInt(colors.size)
        if(grid.all{row->row.all{it<0}}) {
            for(rr in 0..3)for(cc in 0..7)grid[rr][cc]=Random.nextInt(colors.size)
            points+=200
        }
        if(grid[10].any{it>=0} || grid[11].any{it>=0})ended=true
        invalidate()
    }
    override fun onDraw(c:Canvas) {
        super.onDraw(c)
        val rad=radius()
        p.textSize=rad*.85f;p.color=Color.WHITE;p.isFakeBoldText=true
        c.drawText("Pikë: $points",rad,shooterY()+rad*2.5f,p)
        c.drawText("Gjuajtje: $shots",rad*8,shooterY()+rad*2.5f,p)
        for(r in 0..11)for(col in 0..7)if(grid[r][col]>=0) {
            p.color=colors[grid[r][col]]
            c.drawCircle(centerX(col,r),centerY(r),rad*.88f,p)
            p.color=0x66ffffff;p.style=Paint.Style.STROKE;p.strokeWidth=3f
            c.drawCircle(centerX(col,r),centerY(r),rad*.67f,p);p.style=Paint.Style.FILL
        }
        p.color=colors[next];c.drawCircle(width/2f,shooterY(),rad*.9f,p)
        if(moving) {
            p.color=colors[next];c.drawCircle(px,py,rad*.88f,p)
            val now=System.nanoTime()
            val dt=if(lastFrame==0L)0.016f else ((now-lastFrame)/1e9f).coerceIn(0.005f,0.035f)
            lastFrame=now
            px+=vx*dt;py+=vy*dt
            if(px<rad || px>width-rad){vx=-vx;px=px.coerceIn(rad,width-rad)}
            if(py<=rad || (0..11).any{r->(0..7).any{col->grid[r][col]>=0 && hypot(px-centerX(col,r),py-centerY(r))<rad*1.8f}})snap()
            else postInvalidateOnAnimation()
        }
        if(ended) {
            p.color=0xee0a1027.toInt();c.drawRoundRect(rad,shooterY()/3, width-rad,shooterY()/3+rad*6,20f,20f,p)
            p.color=Color.WHITE;p.textSize=rad*1.1f;c.drawText("Fund i lojës",rad*2,shooterY()/3+rad*2,p)
            p.textSize=rad*.7f;c.drawText("Shtyp Rifillo lojën",rad*2,shooterY()/3+rad*4,p)
        }
    }
    override fun onTouchEvent(e:MotionEvent):Boolean {
        if(e.action!=MotionEvent.ACTION_UP)return true
        if(moving || ended)return true
        val dx=e.x-width/2f
        val dy=e.y-shooterY()
        if(dy>=-radius())return true
        val length=hypot(dx,dy).coerceAtLeast(1f)
        px=width/2f;py=shooterY();vx=dx/length*650f;vy=dy/length*650f
        moving=true;lastFrame=0L;postInvalidateOnAnimation()
        return true
    }
}

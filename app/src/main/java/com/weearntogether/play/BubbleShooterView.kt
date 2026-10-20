package com.weearntogether.play

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/** Native Bubble Shooter: saved board, trajectory preview, matching/drop logic and level progression. */
class BubbleShooterView(context: Context): View(context) {
    private val colors=intArrayOf(0xfffe6398.toInt(),0xff52d3fa.toInt(),0xffffca57.toInt(),0xff9c7bff.toInt())
    private val grid=Array(12){IntArray(8){-1}}
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs=context.getSharedPreferences("bubble_shooter",Context.MODE_PRIVATE)
    private var current=Random.nextInt(colors.size)
    private var next=Random.nextInt(colors.size)
    private var shots=0
    private var points=0
    private var level=1
    private var best=prefs.getInt("best",0)
    private var ended=false
    private var moving=false
    private var aimX=0f; private var aimY=0f
    private var px=0f; private var py=0f; private var vx=0f; private var vy=0f
    private var lastFrame=0L

    init { if(!restore()) newBoard() }

    private fun newBoard(){
        for(r in 0..11) grid[r].fill(-1)
        for(r in 0..3) for(c in 0..7) grid[r][c]=Random.nextInt(colors.size)
        current=Random.nextInt(colors.size);next=Random.nextInt(colors.size)
        shots=0;points=0;level=1;ended=false;moving=false;aimX=0f;aimY=0f
        save()
    }
    fun restart(){ newBoard(); invalidate() }

    private fun restore():Boolean {
        val raw=prefs.getString("grid",null) ?: return false
        val cells=raw.split(",").mapNotNull{it.toIntOrNull()}
        if(cells.size!=96 || cells.any{it !in -1 until colors.size}) return false
        for(i in cells.indices) grid[i/8][i%8]=cells[i]
        current=prefs.getInt("current",prefs.getInt("next",0)).coerceIn(0,colors.lastIndex)
        next=prefs.getInt("nextPreview",Random.nextInt(colors.size)).coerceIn(0,colors.lastIndex)
        shots=prefs.getInt("shots",0).coerceAtLeast(0);points=prefs.getInt("points",0).coerceAtLeast(0)
        level=prefs.getInt("level",1).coerceAtLeast(1);ended=prefs.getBoolean("ended",false)
        return true
    }
    private fun save(){
        if(points>best) best=points
        prefs.edit().putString("grid",grid.flatMap{it.asList()}.joinToString(","))
            .putInt("current",current).putInt("nextPreview",next).putInt("shots",shots)
            .putInt("points",points).putInt("level",level).putInt("best",best).putBoolean("ended",ended).apply()
    }

    private fun radius()=width.toFloat()/17f
    private fun centerX(c:Int,r:Int)=radius()*(1+2*c+(r%2))
    private fun centerY(r:Int)=radius()*(1.3f+1.75f*r)
    private fun shooterY()=min(height.toFloat()-radius()*3, radius()*29)
    private fun neighbors(r:Int,c:Int):List<Pair<Int,Int>> {
        val d=if(r%2==0) arrayOf(-1 to -1,-1 to 0,0 to -1,0 to 1,1 to -1,1 to 0) else arrayOf(-1 to 0,-1 to 1,0 to -1,0 to 1,1 to 0,1 to 1)
        return d.map{(dr,dc)->r+dr to c+dc}.filter{(rr,cc)->rr in 0..11 && cc in 0..7}
    }
    private fun group(r:Int,c:Int,matching:Boolean):Set<Pair<Int,Int>> {
        val result=mutableSetOf(r to c);val q=ArrayDeque<Pair<Int,Int>>();q.add(r to c)
        while(q.isNotEmpty()){val(y,x)=q.removeFirst();for((yy,xx) in neighbors(y,x)) if(grid[yy][xx]>=0&&(!matching||grid[yy][xx]==grid[r][c])&&result.add(yy to xx))q.add(yy to xx)}
        return result
    }
    private fun advanceLevel(){
        level++;points+=250*level
        for(r in 0..11)grid[r].fill(-1)
        val rows=(3+(level-1)/2).coerceAtMost(6)
        for(r in 0 until rows)for(c in 0..7)grid[r][c]=Random.nextInt(colors.size)
    }
    private fun snap(){
        moving=false
        val candidates=mutableListOf<Triple<Int,Int,Float>>()
        for(r in 0..11)for(c in 0..7)if(grid[r][c]<0){val connected=r==0||neighbors(r,c).any{(rr,cc)->grid[rr][cc]>=0};if(connected)candidates.add(Triple(r,c,hypot(px-centerX(c,r),py-centerY(r))))}
        val nearest=candidates.minByOrNull{it.third}?:run{ended=true;save();invalidate();return}
        val r=nearest.first;val c=nearest.second;grid[r][c]=current
        val matches=group(r,c,true)
        if(matches.size>=3){
            matches.forEach{(rr,cc)->grid[rr][cc]=-1};points+=matches.size*10*level
            val attached=mutableSetOf<Pair<Int,Int>>();for(col in 0..7)if(grid[0][col]>=0)attached.addAll(group(0,col,false))
            for(rr in 0..11)for(cc in 0..7)if(grid[rr][cc]>=0&&(rr to cc)!in attached){grid[rr][cc]=-1;points+=15*level}
        }
        shots++;current=next;next=Random.nextInt(colors.size)
        if(grid.all{row->row.all{it<0}})advanceLevel()
        if(grid[10].any{it>=0}||grid[11].any{it>=0})ended=true
        save();invalidate()
    }
    private fun drawAimGuide(c:Canvas){
        if(moving||ended||aimY<=0f)return
        val sx=width/2f;val sy=shooterY();val dx=aimX-sx;val dy=aimY-sy;if(dy>=-radius())return
        val len=hypot(dx,dy).coerceAtLeast(1f);var ux=dx/len;val uy=dy/len;var x=sx;var y=sy
        p.color=0x99ffffff.toInt()
        repeat(18){x+=ux*radius()*1.05f;y+=uy*radius()*1.05f;if(x<radius()||x>width-radius()){ux=-ux;x=x.coerceIn(radius(),width-radius())};if(y<radius())return;p.alpha=(210-it*8).coerceAtLeast(55);c.drawCircle(x,y,radius()*.10f,p)}
        p.alpha=255
    }
    override fun onDraw(c:Canvas){
        super.onDraw(c);val rad=radius();p.isFakeBoldText=true;p.color=Color.WHITE;p.textSize=rad*.7f
        c.drawText("Pikë $points",rad,shooterY()+rad*2.2f,p);c.drawText("Rekord $best",rad*5.5f,shooterY()+rad*2.2f,p);c.drawText("Nivel $level",rad*11.5f,shooterY()+rad*2.2f,p)
        for(r in 0..11)for(col in 0..7)if(grid[r][col]>=0){p.color=colors[grid[r][col]];c.drawCircle(centerX(col,r),centerY(r),rad*.88f,p);p.color=0x66ffffff;p.style=Paint.Style.STROKE;p.strokeWidth=3f;c.drawCircle(centerX(col,r),centerY(r),rad*.67f,p);p.style=Paint.Style.FILL}
        drawAimGuide(c)
        p.color=colors[current];c.drawCircle(width/2f,shooterY(),rad*.9f,p)
        p.color=Color.WHITE;p.textSize=rad*.48f;c.drawText("Tjetra",rad*.5f,shooterY()-rad*.5f,p);p.color=colors[next];c.drawCircle(rad*1.5f,shooterY()+rad*.3f,rad*.55f,p)
        if(moving){p.color=colors[current];c.drawCircle(px,py,rad*.88f,p);val now=System.nanoTime();val dt=if(lastFrame==0L).016f else((now-lastFrame)/1e9f).coerceIn(.005f,.035f);lastFrame=now;px+=vx*dt;py+=vy*dt;if(px<rad||px>width-rad){vx=-vx;px=px.coerceIn(rad,width-rad)};if(py<=rad||(0..11).any{r->(0..7).any{col->grid[r][col]>=0&&hypot(px-centerX(col,r),py-centerY(r))<rad*1.8f}})snap()else postInvalidateOnAnimation()}
        if(ended){p.color=0xee0a1027.toInt();c.drawRoundRect(rad,shooterY()/3,width-rad,shooterY()/3+rad*6,20f,20f,p);p.color=0xffffca57.toInt();p.textSize=rad*1.05f;c.drawText("Fund i lojës",rad*2,shooterY()/3+rad*2,p);p.color=Color.WHITE;p.textSize=rad*.65f;c.drawText("Pikë $points • Nivel $level",rad*2,shooterY()/3+rad*3.3f,p);c.drawText("Shtyp Rifillo lojën",rad*2,shooterY()/3+rad*4.5f,p)}
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{
        when(e.action){
            MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{if(!moving&&!ended){aimX=e.x;aimY=e.y;invalidate()};return true}
            MotionEvent.ACTION_UP->{if(moving||ended)return true;aimX=e.x;aimY=e.y;val dx=e.x-width/2f;val dy=e.y-shooterY();if(dy>=-radius())return true;val len=hypot(dx,dy).coerceAtLeast(1f);px=width/2f;py=shooterY();vx=dx/len*650f;vy=dy/len*650f;moving=true;lastFrame=0L;postInvalidateOnAnimation();return true}
        }
        return true
    }
}

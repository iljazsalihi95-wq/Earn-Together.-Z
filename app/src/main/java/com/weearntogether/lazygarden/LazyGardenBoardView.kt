package com.weearntogether.lazygarden

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class LazyGardenBoardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var engine = LazyGardenEngine(LazyGardenLevels.get(1)); private set
    var onMove: ((MatchResult) -> Unit)? = null
    private var selected: Cell? = null
    private var inputEnabled = true
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colors = intArrayOf(0xFF55A96B.toInt(),0xFFD968A0.toInt(),0xFFB9475F.toInt(),0xFF714BC7.toInt(),0xFFE6B84F.toInt(),0xFF4EA9CF.toInt())

    fun loadLevel(level:LazyGardenLevel){ engine=LazyGardenEngine(level); selected=null; inputEnabled=true; invalidate() }
    fun setInputEnabled(enabled:Boolean){ inputEnabled=enabled; selected=null; invalidate() }

    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val size=min(width.toFloat()/engine.level.columns,height.toFloat()/engine.level.rows)
        val ox=(width-size*engine.level.columns)/2f; val oy=(height-size*engine.level.rows)/2f
        for(r in 0 until engine.level.rows) for(c in 0 until engine.level.columns){
            val left=ox+c*size; val top=oy+r*size
            paint.color=0x332FFFFF; canvas.drawRoundRect(RectF(left+3,top+3,left+size-3,top+size-3),size*.2f,size*.2f,paint)
            val tile=engine.board[r][c]; paint.color=colors[tile.ordinal]
            canvas.drawCircle(left+size/2,top+size/2,size*.34f,paint)
            paint.color=0x55FFFFFF; canvas.drawCircle(left+size*.40f,top+size*.36f,size*.10f,paint)
            drawObstacle(canvas,engine.obstacleAt(Cell(r,c)),left,top,size)
            if(selected==Cell(r,c)){
                paint.style=Paint.Style.STROKE; paint.strokeWidth=5f; paint.color=0xFFF6D36F.toInt()
                canvas.drawRoundRect(RectF(left+2,top+2,left+size-2,top+size-2),size*.2f,size*.2f,paint); paint.style=Paint.Style.FILL
            }
        }
    }

    private fun drawObstacle(canvas:Canvas, obstacle:GardenObstacle?, left:Float, top:Float, size:Float){
        when(obstacle){
            GardenObstacle.CRATE -> {
                paint.color=0xFFD69A55.toInt(); canvas.drawRoundRect(RectF(left+size*.14f,top+size*.14f,left+size*.86f,top+size*.86f),size*.08f,size*.08f,paint)
                paint.style=Paint.Style.STROKE; paint.strokeWidth=size*.08f; paint.color=0xFF8A572F.toInt()
                canvas.drawLine(left+size*.25f,top+size*.25f,left+size*.75f,top+size*.75f,paint); canvas.drawLine(left+size*.75f,top+size*.25f,left+size*.25f,top+size*.75f,paint); paint.style=Paint.Style.FILL
            }
            GardenObstacle.IVY -> {
                paint.color=0xCC277A4A.toInt(); canvas.drawCircle(left+size*.34f,top+size*.38f,size*.18f,paint); canvas.drawCircle(left+size*.62f,top+size*.55f,size*.21f,paint); canvas.drawCircle(left+size*.43f,top+size*.70f,size*.16f,paint)
            }
            GardenObstacle.STONE -> {
                paint.color=0xFF6F7488.toInt(); canvas.drawRoundRect(RectF(left+size*.12f,top+size*.18f,left+size*.88f,top+size*.82f),size*.22f,size*.22f,paint)
                paint.color=0x559FFFFFF; canvas.drawCircle(left+size*.38f,top+size*.38f,size*.10f,paint)
            }
            null -> Unit
        }
    }

    override fun onTouchEvent(event:MotionEvent):Boolean{
        if(!inputEnabled) return true
        if(event.action!=MotionEvent.ACTION_UP) return true
        val size=min(width.toFloat()/engine.level.columns,height.toFloat()/engine.level.rows)
        val ox=(width-size*engine.level.columns)/2f; val oy=(height-size*engine.level.rows)/2f
        val c=((event.x-ox)/size).toInt(); val r=((event.y-oy)/size).toInt()
        if(r !in 0 until engine.level.rows || c !in 0 until engine.level.columns) return true
        val cell=Cell(r,c)
        if(engine.obstacleAt(cell)==GardenObstacle.STONE){ selected=null; invalidate(); return true }
        val previous=selected
        if(previous==null) selected=cell else if(previous==cell) selected=null else {
            val result=engine.swap(previous,cell)
            if(result!=null){ selected=null; onMove?.invoke(result) } else selected=cell
        }
        invalidate(); return true
    }
}

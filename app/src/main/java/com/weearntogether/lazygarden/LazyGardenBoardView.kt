package com.weearntogether.lazygarden

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.weearntogether.play.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

class LazyGardenBoardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var engine = LazyGardenEngine(LazyGardenLevels.get(1), Random(levelSeed(1))); private set
    var onMove: ((MatchResult) -> Unit)? = null
    private var selected: Cell? = null
    private var inputEnabled = true
    private var downCell: Cell? = null
    private var downX = 0f
    private var downY = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    @Suppress("DEPRECATION")
    private fun drawable(id: Int): Drawable = requireNotNull(resources.getDrawable(id, context.theme))

    private val tileArt: Map<GardenTile, Drawable> = mapOf(
        GardenTile.LEAF to drawable(R.drawable.lg_tile_leaf),
        GardenTile.FLOWER to drawable(R.drawable.lg_tile_flower),
        GardenTile.ROSE to drawable(R.drawable.lg_tile_rose),
        GardenTile.BERRY to drawable(R.drawable.lg_tile_berry),
        GardenTile.PEAR to drawable(R.drawable.lg_tile_pear),
        GardenTile.WATER to drawable(R.drawable.lg_tile_water)
    )
    private val obstacleArt: Map<GardenObstacle, Drawable> = mapOf(
        GardenObstacle.CRATE to drawable(R.drawable.lg_obstacle_crate),
        GardenObstacle.IVY to drawable(R.drawable.lg_obstacle_ivy),
        GardenObstacle.STONE to drawable(R.drawable.lg_obstacle_stone)
    )

    fun loadLevel(level:LazyGardenLevel){engine=LazyGardenEngine(level,Random(levelSeed(level.number)));selected=null;downCell=null;inputEnabled=true;invalidate()}
    fun setInputEnabled(enabled:Boolean){ inputEnabled=enabled; selected=null; downCell=null; invalidate() }

    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val size=cellSize(); val ox=(width-size*engine.level.columns)/2f; val oy=(height-size*engine.level.rows)/2f
        for(r in 0 until engine.level.rows) for(c in 0 until engine.level.columns){
            val left=ox+c*size; val top=oy+r*size
            paint.color=if((r+c)%2==0)0x283C2C72 else 0x28305AA0
            canvas.drawRoundRect(RectF(left+3,top+3,left+size-3,top+size-3),size*.20f,size*.20f,paint)
            val inset=(size*.08f).toInt()
            tileArt.getValue(engine.board[r][c]).apply { setBounds((left+inset).toInt(),(top+inset).toInt(),(left+size-inset).toInt(),(top+size-inset).toInt()); draw(canvas) }
            drawObstacle(canvas,engine.obstacleAt(Cell(r,c)),left,top,size)
            if(selected==Cell(r,c)){paint.style=Paint.Style.STROKE;paint.strokeWidth=5f;paint.color=0xFFF6D36F.toInt();canvas.drawRoundRect(RectF(left+2,top+2,left+size-2,top+size-2),size*.2f,size*.2f,paint);paint.style=Paint.Style.FILL}
        }
    }

    private fun drawObstacle(canvas:Canvas, obstacle:GardenObstacle?, left:Float, top:Float, size:Float){
        if(obstacle==null)return
        val inset=(size*.06f).toInt()
        obstacleArt.getValue(obstacle).apply{setBounds((left+inset).toInt(),(top+inset).toInt(),(left+size-inset).toInt(),(top+size-inset).toInt());draw(canvas)}
    }

    override fun onTouchEvent(event:MotionEvent):Boolean{if(!inputEnabled)return true;when(event.actionMasked){
        MotionEvent.ACTION_DOWN->{downX=event.x;downY=event.y;downCell=cellAt(event.x,event.y);downCell?.let{if(engine.obstacleAt(it)!=GardenObstacle.STONE)selected=it};invalidate();return true}
        MotionEvent.ACTION_CANCEL->{downCell=null;selected=null;invalidate();return true}
        MotionEvent.ACTION_UP->{val start=downCell;downCell=null;if(start==null){selected=null;invalidate();return true};if(engine.obstacleAt(start)==GardenObstacle.STONE){selected=null;invalidate();return true};val dx=event.x-downX;val dy=event.y-downY;val threshold=cellSize()*.22f;if(abs(dx)>=threshold||abs(dy)>=threshold){val target=if(abs(dx)>abs(dy))Cell(start.row,start.col+if(dx>0)1 else -1)else Cell(start.row+if(dy>0)1 else -1,start.col);trySwap(start,target)}else{val tapped=cellAt(event.x,event.y);if(tapped==null||engine.obstacleAt(tapped)==GardenObstacle.STONE)selected=null else{val previous=selected;if(previous!=null&&previous!=tapped&&abs(previous.row-tapped.row)+abs(previous.col-tapped.col)==1)trySwap(previous,tapped)else selected=tapped}};invalidate();return true}}
        return true}

    private fun trySwap(a:Cell,b:Cell){val result=engine.swap(a,b);if(result!=null){selected=null;performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);onMove?.invoke(result)}else{selected=a;performHapticFeedback(HapticFeedbackConstants.REJECT)}}
    private fun cellSize():Float=min(width.toFloat()/engine.level.columns,height.toFloat()/engine.level.rows)
    private fun cellAt(x:Float,y:Float):Cell?{val size=cellSize();if(size<=0f)return null;val ox=(width-size*engine.level.columns)/2f;val oy=(height-size*engine.level.rows)/2f;val c=((x-ox)/size).toInt();val r=((y-oy)/size).toInt();return if(r in 0 until engine.level.rows&&c in 0 until engine.level.columns)Cell(r,c)else null}

    companion object { private fun levelSeed(level:Int):Int = 0x4C475244 xor (level * 1103515245) }
}

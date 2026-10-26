package com.weearntogether.lazygarden

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.weearntogether.play.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class LazyGardenBoardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var engine = LazyGardenEngine(LazyGardenLevels.get(1), Random(levelSeed(1))); private set
    var onMove: ((MatchResult) -> Unit)? = null
    var onPowerTarget: ((Cell) -> Unit)? = null
    private var selected: Cell? = null
    private var powerTargetMode = false
    private var inputEnabled = true
    private var downCell: Cell? = null
    private var downX = 0f
    private var downY = 0f
    private var swapPulse: Set<Cell> = emptySet()
    private var swapPulseUntil = 0L
    private var clearPulse: Set<Cell> = emptySet()
    private var clearPulseUntil = 0L
    private var invalidCell: Cell? = null
    private var invalidPulseUntil = 0L
    private var lastInteractionAt = SystemClock.uptimeMillis()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    @Suppress("DEPRECATION") private fun drawable(id: Int): Drawable = requireNotNull(resources.getDrawable(id, context.theme))
    private val tileArt=mapOf(GardenTile.LEAF to drawable(R.drawable.lg_tile_leaf),GardenTile.FLOWER to drawable(R.drawable.lg_tile_flower),GardenTile.ROSE to drawable(R.drawable.lg_tile_rose),GardenTile.BERRY to drawable(R.drawable.lg_tile_berry),GardenTile.PEAR to drawable(R.drawable.lg_tile_pear),GardenTile.WATER to drawable(R.drawable.lg_tile_water))
    private val obstacleArt=mapOf(GardenObstacle.CRATE to drawable(R.drawable.lg_obstacle_crate),GardenObstacle.IVY to drawable(R.drawable.lg_obstacle_ivy),GardenObstacle.STONE to drawable(R.drawable.lg_obstacle_stone))

    fun loadLevel(level:LazyGardenLevel){engine=LazyGardenEngine(level,Random(levelSeed(level.number)));selected=null;powerTargetMode=false;downCell=null;swapPulse=emptySet();clearPulse=emptySet();invalidCell=null;inputEnabled=true;lastInteractionAt=SystemClock.uptimeMillis();scheduleHint();invalidate()}
    fun setInputEnabled(enabled:Boolean){inputEnabled=enabled;selected=null;powerTargetMode=false;downCell=null;lastInteractionAt=SystemClock.uptimeMillis();if(enabled)scheduleHint();invalidate()}
    fun setPowerTargetMode(enabled:Boolean){powerTargetMode=enabled&&inputEnabled;selected=null;downCell=null;lastInteractionAt=SystemClock.uptimeMillis();performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);invalidate()}
    fun showPowerClear(cells:Set<Cell>){clearPulse=cells;clearPulseUntil=SystemClock.uptimeMillis()+CLEAR_PULSE_MS;performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);postInvalidateOnAnimation()}

    override fun onDraw(canvas:Canvas){super.onDraw(canvas);val now=SystemClock.uptimeMillis();val hint=if(inputEnabled&&!powerTargetMode&&selected==null&&now-lastInteractionAt>=HINT_DELAY_MS)engine.findHint()else null;val hintCells=if(hint==null)emptySet()else setOf(hint.first,hint.second);val animating=now<swapPulseUntil||now<clearPulseUntil||now<invalidPulseUntil||hint!=null;val size=cellSize();val ox=(width-size*engine.level.columns)/2f;val oy=(height-size*engine.level.rows)/2f
        for(r in 0 until engine.level.rows)for(c in 0 until engine.level.columns){val cell=Cell(r,c);val left=ox+c*size;val top=oy+r*size;paint.color=if((r+c)%2==0)0x283C2C72 else 0x28305AA0;canvas.drawRoundRect(RectF(left+3,top+3,left+size-3,top+size-3),size*.20f,size*.20f,paint);val pulseScale=if(cell in swapPulse&&now<swapPulseUntil){val p=((swapPulseUntil-now).coerceIn(0L,SWAP_PULSE_MS).toFloat()/SWAP_PULSE_MS);1f+0.10f*sin((1f-p)*Math.PI).toFloat()}else 1f;val inset=size*.08f+size*(1f-pulseScale)/2f;tileArt.getValue(engine.board[r][c]).apply{setBounds((left+inset).toInt(),(top+inset).toInt(),(left+size-inset).toInt(),(top+size-inset).toInt());draw(canvas)};drawObstacle(canvas,cell,left,top,size);if(cell in clearPulse&&now<clearPulseUntil)drawClearBurst(canvas,left,top,size,now);if(cell in hintCells){val wave=(.55f+.45f*sin(now/170.0).toFloat()).coerceIn(0f,1f);paint.style=Paint.Style.STROKE;paint.strokeWidth=3f+2f*wave;paint.color=((0x90+((0x60*wave).toInt())) shl 24) or 0x00FFD86B;canvas.drawRoundRect(RectF(left+3,top+3,left+size-3,top+size-3),size*.20f,size*.20f,paint);paint.style=Paint.Style.FILL};if(powerTargetMode){paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;paint.color=0xCC78E6FF.toInt();canvas.drawRoundRect(RectF(left+5,top+5,left+size-5,top+size-5),size*.20f,size*.20f,paint);paint.style=Paint.Style.FILL};if(selected==cell){paint.style=Paint.Style.STROKE;paint.strokeWidth=5f;paint.color=0xFFF6D36F.toInt();canvas.drawRoundRect(RectF(left+2,top+2,left+size-2,top+size-2),size*.2f,size*.2f,paint);paint.style=Paint.Style.FILL};if(invalidCell==cell&&now<invalidPulseUntil){val remaining=((invalidPulseUntil-now).coerceAtLeast(0L).toFloat()/INVALID_PULSE_MS).coerceIn(0f,1f);paint.style=Paint.Style.STROKE;paint.strokeWidth=4f+2f*remaining;paint.color=0xFFE95C6B.toInt();canvas.drawRoundRect(RectF(left+4,top+4,left+size-4,top+size-4),size*.2f,size*.2f,paint);paint.style=Paint.Style.FILL}}
        if(animating)postInvalidateOnAnimation()else{if(now>=swapPulseUntil)swapPulse=emptySet();if(now>=clearPulseUntil)clearPulse=emptySet();if(now>=invalidPulseUntil)invalidCell=null}}

    private fun drawClearBurst(canvas:Canvas,left:Float,top:Float,size:Float,now:Long){val remaining=((clearPulseUntil-now).coerceIn(0L,CLEAR_PULSE_MS).toFloat()/CLEAR_PULSE_MS).coerceIn(0f,1f);val progress=1f-remaining;val cx=left+size/2f;val cy=top+size/2f;val radius=size*(.16f+.46f*progress);paint.style=Paint.Style.STROKE;paint.strokeWidth=size*(.10f*(1f-progress)+.025f);paint.color=(((0xE0*remaining).toInt().coerceIn(0,255)) shl 24) or 0x00FFF1A8;canvas.drawCircle(cx,cy,radius,paint);paint.style=Paint.Style.FILL;paint.color=(((0x90*remaining).toInt().coerceIn(0,255)) shl 24) or 0x00FFFFFF;canvas.drawCircle(cx,cy,size*.10f*(1f-progress),paint)}

    private fun drawObstacle(canvas:Canvas,cell:Cell,left:Float,top:Float,size:Float){val obstacle=engine.obstacleAt(cell)?:return;val inset=(size*.06f).toInt();obstacleArt.getValue(obstacle).apply{setBounds((left+inset).toInt(),(top+inset).toInt(),(left+size-inset).toInt(),(top+size-inset).toInt());draw(canvas)};val hits=engine.obstacleHitsLeft(cell);if(hits>1){paint.style=Paint.Style.FILL;paint.color=0xD91D2636.toInt();canvas.drawCircle(left+size*.78f,top+size*.22f,size*.14f,paint);paint.style=Paint.Style.STROKE;paint.strokeWidth=size*.025f;paint.color=0xFFF6D36F.toInt();canvas.drawCircle(left+size*.78f,top+size*.22f,size*.14f,paint);paint.style=Paint.Style.FILL;paint.color=0xFFFFFFFF.toInt();paint.textAlign=Paint.Align.CENTER;paint.textSize=size*.22f;paint.isFakeBoldText=true;canvas.drawText(hits.toString(),left+size*.78f,top+size*.29f,paint);paint.isFakeBoldText=false;paint.textAlign=Paint.Align.LEFT};if(hits==1&&obstacle!=GardenObstacle.IVY){paint.style=Paint.Style.STROKE;paint.strokeWidth=size*.045f;paint.color=0xFFFFD98A.toInt();canvas.drawLine(left+size*.24f,top+size*.22f,left+size*.52f,top+size*.55f,paint);canvas.drawLine(left+size*.52f,top+size*.55f,left+size*.38f,top+size*.78f,paint);paint.style=Paint.Style.FILL}}
    private fun isBlocked(cell:Cell):Boolean=engine.obstacleAt(cell)==GardenObstacle.STONE||engine.obstacleAt(cell)==GardenObstacle.CRATE

    override fun onTouchEvent(event:MotionEvent):Boolean{
        if(!inputEnabled)return true
        if(powerTargetMode){if(event.actionMasked==MotionEvent.ACTION_UP){val target=cellAt(event.x,event.y);lastInteractionAt=SystemClock.uptimeMillis();if(target!=null){powerTargetMode=false;onPowerTarget?.invoke(target)}else{performHapticFeedback(HapticFeedbackConstants.REJECT)};invalidate()};return true}
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN->{parent?.requestDisallowInterceptTouchEvent(true);lastInteractionAt=SystemClock.uptimeMillis();scheduleHint();downX=event.x;downY=event.y;downCell=cellAt(event.x,event.y);downCell?.let{if(!isBlocked(it))selected=it};invalidate();return true}
            MotionEvent.ACTION_CANCEL->{parent?.requestDisallowInterceptTouchEvent(false);downCell=null;selected=null;invalidate();return true}
            MotionEvent.ACTION_UP->{parent?.requestDisallowInterceptTouchEvent(false);val start=downCell;downCell=null;if(start==null){selected=null;invalidate();return true};if(isBlocked(start)){selected=null;showInvalid(start);return true};val dx=event.x-downX;val dy=event.y-downY;val threshold=cellSize()*.22f;if(abs(dx)>=threshold||abs(dy)>=threshold){val target=if(abs(dx)>abs(dy))Cell(start.row,start.col+if(dx>0)1 else -1)else Cell(start.row+if(dy>0)1 else -1,start.col);trySwap(start,target)}else{val tapped=cellAt(event.x,event.y);if(tapped==null||isBlocked(tapped)){selected=null;tapped?.let(::showInvalid)}else{val previous=selected;if(previous!=null&&previous!=tapped&&abs(previous.row-tapped.row)+abs(previous.col-tapped.col)==1)trySwap(previous,tapped)else selected=tapped}};invalidate();return true}
        };return true
    }
    private fun trySwap(a:Cell,b:Cell){val result=engine.swap(a,b);if(result!=null){selected=null;invalidCell=null;swapPulse=setOf(a,b);swapPulseUntil=SystemClock.uptimeMillis()+SWAP_PULSE_MS;clearPulse=result.cells;clearPulseUntil=SystemClock.uptimeMillis()+CLEAR_PULSE_MS;lastInteractionAt=SystemClock.uptimeMillis();scheduleHint();performHapticFeedback(if(result.cells.size>=5)HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CLOCK_TICK);onMove?.invoke(result);postInvalidateOnAnimation()}else{selected=a;showInvalid(a)}}
    private fun showInvalid(cell:Cell){invalidCell=cell;invalidPulseUntil=SystemClock.uptimeMillis()+INVALID_PULSE_MS;performHapticFeedback(HapticFeedbackConstants.REJECT);postInvalidateOnAnimation()}
    private fun scheduleHint(){postDelayed({if(inputEnabled&&!powerTargetMode&&selected==null)invalidate()},HINT_DELAY_MS+40L)}
    private fun cellSize():Float=min(width.toFloat()/engine.level.columns,height.toFloat()/engine.level.rows)
    private fun cellAt(x:Float,y:Float):Cell?{val size=cellSize();if(size<=0f)return null;val ox=(width-size*engine.level.columns)/2f;val oy=(height-size*engine.level.rows)/2f;val c=((x-ox)/size).toInt();val r=((y-oy)/size).toInt();return if(r in 0 until engine.level.rows&&c in 0 until engine.level.columns)Cell(r,c)else null}
    companion object{private const val SWAP_PULSE_MS=190L;private const val CLEAR_PULSE_MS=360L;private const val INVALID_PULSE_MS=230L;private const val HINT_DELAY_MS=4200L;private fun levelSeed(level:Int):Int=0x4C475244 xor(level*1103515245)}
}
package com.weearntogether.lazygarden

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.sin

/** Original layered 2D garden scene with persistent restorations and animated worker. */
class LazyGardenSceneView(context: Context) : View(context) {
    enum class WorkerState { IDLE, WALK, WORK, CELEBRATE }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var completed: Set<Int> = emptySet()
    private var workerX = .18f
    private var workerState = WorkerState.IDLE
    private var motionPhase = 0f
    private var animator: ValueAnimator? = null

    fun setCompletedTasks(levels: Set<Int>) { completed = levels; invalidate() }

    fun playRestoration(level: Int, onFinished: () -> Unit = {}) {
        animator?.cancel()
        val target = when(level) { 1,2 -> .48f; 3,8 -> .25f; 4,7,9 -> .78f; else -> .52f }
        workerState = WorkerState.WALK
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2600
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val p = it.animatedFraction
                motionPhase = p * 18f
                workerX = if (p < .42f) .18f + (target - .18f) * (p / .42f) else target
                workerState = when { p < .42f -> WorkerState.WALK; p < .82f -> WorkerState.WORK; else -> WorkerState.CELEBRATE }
                if (p >= .82f && level !in completed) completed = completed + level
                invalidate()
                if (p >= 1f) { workerState = WorkerState.IDLE; invalidate(); onFinished() }
            }
            start()
        }
    }

    override fun onDetachedFromWindow() { animator?.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        drawBackdrop(c,w,h)
        if (2 in completed) drawPath(c,w,h)
        drawHedges(c,w,h)
        if (1 in completed) drawGate(c,w,h) else drawBrokenGate(c,w,h)
        if (3 in completed) drawFlowers(c,w,h)
        if (4 in completed) drawBench(c,w,h)
        if (5 in completed) drawFountain(c,w,h)
        if (6 in completed) drawRoses(c,w,h)
        if (7 in completed) drawLanterns(c,w,h)
        if (8 in completed) drawPond(c,w,h)
        if (9 in completed) drawRelaxCorner(c,w,h)
        if (10 in completed) drawCenter(c,w,h)
        drawWorker(c,w,h)
    }

    private fun drawBackdrop(c:Canvas,w:Float,h:Float) {
        paint.shader = LinearGradient(0f,0f,0f,h*.52f, Color.rgb(111,196,239), Color.rgb(221,245,238), Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,w,h*.52f,paint); paint.shader=null
        paint.color=0x33FFFFFF; c.drawOval(RectF(w*.08f,h*.09f,w*.31f,h*.2f),paint); c.drawOval(RectF(w*.66f,h*.06f,w*.91f,h*.18f),paint)
        paint.shader = LinearGradient(0f,h*.36f,0f,h,0xFF5AA45B.toInt(),0xFF24633E.toInt(),Shader.TileMode.CLAMP)
        c.drawRect(0f,h*.36f,w,h,paint); paint.shader=null
        paint.color=0x22412620; c.drawOval(RectF(-w*.1f,h*.82f,w*1.1f,h*1.12f),paint)
    }

    private fun drawHedges(c:Canvas,w:Float,h:Float) {
        paint.color=0xFF1F6A3B.toInt()
        for(i in 0..7){ val x=w*(i/7f); c.drawCircle(x,h*.39f,h*.115f,paint) }
        paint.color=0xFF3F9651.toInt()
        for(i in 0..8){ val x=w*(i/8f); c.drawCircle(x,h*.42f,h*.075f,paint) }
    }

    private fun drawWorker(c:Canvas,w:Float,h:Float) {
        val x=w*workerX; val base=h*.87f
        val bob=if(workerState==WorkerState.WALK) sin(motionPhase)*3f else 0f
        val arm=when(workerState){ WorkerState.WORK -> sin(motionPhase)*12f; WorkerState.CELEBRATE -> -16f; else -> 0f }
        paint.color=0x44000000; c.drawOval(RectF(x-22,base+5,x+22,base+14),paint)
        paint.color=0xFFF1C6A3.toInt(); c.drawCircle(x,base-64+bob,13f,paint)
        paint.color=0xFF35251F.toInt(); c.drawArc(RectF(x-14,base-79+bob,x+14,base-52+bob),180f,180f,true,paint)
        paint.color=0xFF315EA8.toInt(); c.drawRoundRect(RectF(x-14,base-51+bob,x+14,base-15+bob),8f,8f,paint)
        paint.color=0xFFF2D35C.toInt(); c.drawRoundRect(RectF(x-12,base-48+bob,x+12,base-41+bob),3f,3f,paint)
        paint.strokeWidth=7f; paint.strokeCap=Paint.Cap.ROUND; paint.color=0xFFF1C6A3.toInt()
        c.drawLine(x-9,base-44+bob,x-20,base-26+bob+arm,paint); c.drawLine(x+9,base-44+bob,x+20,base-26+bob-arm,paint)
        paint.color=0xFF27354D.toInt(); val leg=if(workerState==WorkerState.WALK) sin(motionPhase)*7f else 0f
        c.drawLine(x-7,base-15+bob,x-10-leg,base+4,paint); c.drawLine(x+7,base-15+bob,x+10+leg,base+4,paint)
        if(workerState==WorkerState.WORK){ paint.color=0xFF9B6B3E.toInt();paint.strokeWidth=5f;c.drawLine(x+19,base-31,x+31,base-11,paint);paint.color=0xFFD6DEE5.toInt();c.drawRoundRect(RectF(x+26,base-35,x+36,base-22),2f,2f,paint) }
        paint.strokeCap=Paint.Cap.BUTT
    }

    private fun drawBrokenGate(c:Canvas,w:Float,h:Float){ paint.color=0xFF654831.toInt();paint.strokeWidth=9f;c.drawLine(w*.44f,h*.34f,w*.41f,h*.68f,paint);c.drawLine(w*.59f,h*.35f,w*.55f,h*.64f,paint) }
    private fun drawGate(c:Canvas,w:Float,h:Float){ paint.color=0xFF68452D.toInt();c.drawRoundRect(RectF(w*.385f,h*.29f,w*.43f,h*.69f),5f,5f,paint);c.drawRoundRect(RectF(w*.57f,h*.29f,w*.615f,h*.69f),5f,5f,paint);paint.color=0xFFC99A54.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=6f;val p=Path();p.moveTo(w*.407f,h*.46f);p.quadTo(w*.5f,h*.22f,w*.593f,h*.46f);c.drawPath(p,paint);paint.style=Paint.Style.FILL }
    private fun drawPath(c:Canvas,w:Float,h:Float){ paint.color=0xFFD9C8A5.toInt();val p=Path();p.moveTo(w*.34f,h);p.lineTo(w*.465f,h*.56f);p.lineTo(w*.54f,h*.56f);p.lineTo(w*.72f,h);p.close();c.drawPath(p,paint);paint.color=0x55FFFFFF;c.drawOval(RectF(w*.45f,h*.65f,w*.57f,h*.7f),paint);c.drawOval(RectF(w*.5f,h*.78f,w*.66f,h*.84f),paint) }
    private fun drawFlowers(c:Canvas,w:Float,h:Float){ for(i in 0..7){ val x=w*(.08f+i*.04f);paint.color=0xFF236B39.toInt();c.drawRect(x-1,h*.7f,x+1,h*.79f,paint);paint.color=if(i%2==0)0xFFF15F98.toInt() else 0xFFFFD75B.toInt();c.drawCircle(x,h*.69f,7f,paint);paint.color=0xFFFFF0A8.toInt();c.drawCircle(x,h*.69f,2.5f,paint)} }
    private fun drawBench(c:Canvas,w:Float,h:Float){ paint.color=0x55301C12;c.drawOval(RectF(w*.67f,h*.79f,w*.94f,h*.87f),paint);paint.color=0xFF9A6539.toInt();c.drawRoundRect(RectF(w*.68f,h*.65f,w*.92f,h*.71f),7f,7f,paint);c.drawRoundRect(RectF(w*.69f,h*.73f,w*.91f,h*.77f),5f,5f,paint);paint.color=0xFF3D332D.toInt();c.drawRect(w*.71f,h*.77f,w*.735f,h*.84f,paint);c.drawRect(w*.865f,h*.77f,w*.89f,h*.84f,paint) }
    private fun drawFountain(c:Canvas,w:Float,h:Float){ paint.color=0x445E4430;c.drawOval(RectF(w*.38f,h*.77f,w*.63f,h*.89f),paint);paint.color=0xFFD5D9D4.toInt();c.drawOval(RectF(w*.385f,h*.69f,w*.615f,h*.82f),paint);paint.color=0xFF6BC9EE.toInt();c.drawOval(RectF(w*.405f,h*.7f,w*.595f,h*.785f),paint);paint.color=0xFFC2C8C5.toInt();c.drawRoundRect(RectF(w*.485f,h*.54f,w*.515f,h*.72f),5f,5f,paint);paint.color=0xFF7BD7F2.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=4f;c.drawArc(RectF(w*.445f,h*.48f,w*.555f,h*.66f),195f,150f,false,paint);paint.style=Paint.Style.FILL }
    private fun drawRoses(c:Canvas,w:Float,h:Float){ for(i in 0..8){val x=w*(.32f+i*.047f);paint.color=0xFF225F37.toInt();c.drawCircle(x,h*.84f,10f,paint);paint.color=0xFFE34467.toInt();c.drawCircle(x,h*.81f,6f,paint)} }
    private fun drawLanterns(c:Canvas,w:Float,h:Float){ listOf(.29f,.71f).forEach{x->paint.color=0xFF29323A.toInt();c.drawRoundRect(RectF(w*x-3,h*.48f,w*x+3,h*.75f),3f,3f,paint);paint.color=0xFFFFD76B.toInt();c.drawCircle(w*x,h*.47f,10f,paint);paint.color=0x55FFD76B;c.drawCircle(w*x,h*.47f,17f,paint)} }
    private fun drawPond(c:Canvas,w:Float,h:Float){ paint.color=0x55331D15;c.drawOval(RectF(w*.045f,h*.84f,w*.35f,h*1.01f),paint);paint.color=0xFF54BDD0.toInt();c.drawOval(RectF(w*.055f,h*.81f,w*.34f,h*.97f),paint);paint.color=0xFF75AD4B.toInt();c.drawCircle(w*.18f,h*.88f,10f,paint);paint.color=0xFFF4C9DE.toInt();c.drawCircle(w*.18f,h*.875f,4f,paint) }
    private fun drawRelaxCorner(c:Canvas,w:Float,h:Float){ paint.color=0x44FFFFFF;c.drawCircle(w*.84f,h*.87f,31f,paint);paint.color=0xFFF2E1B2.toInt();c.drawCircle(w*.84f,h*.86f,25f,paint);paint.color=0xFF6B4530.toInt();c.drawRoundRect(RectF(w*.825f,h*.86f,w*.855f,h*.96f),5f,5f,paint) }
    private fun drawCenter(c:Canvas,w:Float,h:Float){ paint.color=0x33FFD96A;c.drawCircle(w*.5f,h*.77f,58f,paint);paint.color=0xFFFFD96A.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=5f;c.drawCircle(w*.5f,h*.77f,52f,paint);paint.style=Paint.Style.FILL }
}

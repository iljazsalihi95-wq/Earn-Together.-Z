package com.weearntogether.lazygarden

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/** Garden scene with persistent restorations and an original animated worker. */
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
                workerState = when {
                    p < .42f -> WorkerState.WALK
                    p < .82f -> WorkerState.WORK
                    else -> WorkerState.CELEBRATE
                }
                invalidate()
                if (p >= .82f && level !in completed) completed = completed + level
                if (p >= 1f) { workerState = WorkerState.IDLE; invalidate(); onFinished() }
            }
            start()
        }
    }

    override fun onDetachedFromWindow() { animator?.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        paint.color = 0xFF9ED8F5.toInt(); c.drawRect(0f, 0f, w, h * .38f, paint)
        paint.color = 0xFF3E8B4F.toInt(); c.drawRect(0f, h * .38f, w, h, paint)
        paint.color = 0xFF245C35.toInt(); c.drawCircle(w*.12f,h*.38f,h*.18f,paint); c.drawCircle(w*.88f,h*.38f,h*.19f,paint)
        if (1 in completed) drawGate(c,w,h) else drawBrokenGate(c,w,h)
        if (2 in completed) drawPath(c,w,h); if (3 in completed) drawFlowers(c,w,h); if (4 in completed) drawBench(c,w,h)
        if (5 in completed) drawFountain(c,w,h); if (6 in completed) drawRoses(c,w,h); if (7 in completed) drawLanterns(c,w,h)
        if (8 in completed) drawPond(c,w,h); if (9 in completed) drawRelaxCorner(c,w,h); if (10 in completed) drawCenter(c,w,h)
        drawWorker(c,w,h)
    }

    private fun drawWorker(c:Canvas,w:Float,h:Float) {
        val x=w*workerX; val base=h*.84f
        val bob=if(workerState==WorkerState.WALK) kotlin.math.sin(motionPhase)*3f else 0f
        val arm=when(workerState){ WorkerState.WORK -> kotlin.math.sin(motionPhase)*12f; WorkerState.CELEBRATE -> -16f; else -> 0f }
        paint.color=0x33000000; c.drawOval(RectF(x-20,base+5,x+20,base+13),paint)
        paint.color=0xFFF0C7A5.toInt(); c.drawCircle(x,base-64+bob,13f,paint)
        paint.color=0xFF3B2B26.toInt(); c.drawArc(RectF(x-14,base-79+bob,x+14,base-52+bob),180f,180f,true,paint)
        paint.color=0xFF315EA8.toInt(); c.drawRoundRect(RectF(x-13,base-50+bob,x+13,base-15+bob),7f,7f,paint)
        paint.strokeWidth=7f; paint.strokeCap=Paint.Cap.ROUND
        paint.color=0xFFF0C7A5.toInt(); c.drawLine(x-9,base-44+bob,x-20,base-26+bob+arm,paint); c.drawLine(x+9,base-44+bob,x+20,base-26+bob-arm,paint)
        paint.color=0xFF27354D.toInt(); val leg=if(workerState==WorkerState.WALK) kotlin.math.sin(motionPhase)*7f else 0f;c.drawLine(x-7,base-15+bob,x-10-leg,base+4,paint);c.drawLine(x+7,base-15+bob,x+10+leg,base+4,paint)
        if(workerState==WorkerState.WORK){ paint.color=0xFF9B6B3E.toInt();paint.strokeWidth=5f;c.drawLine(x+19,base-31,x+31,base-11,paint);paint.color=0xFFBBC4CC.toInt();c.drawRect(x+26,base-35,x+35,base-23,paint) }
        paint.strokeCap=Paint.Cap.BUTT
    }

    private fun drawBrokenGate(c:Canvas,w:Float,h:Float){ paint.color=0xFF6D523A.toInt();paint.strokeWidth=8f;c.drawLine(w*.44f,h*.34f,w*.42f,h*.66f,paint);c.drawLine(w*.58f,h*.36f,w*.55f,h*.62f,paint) }
    private fun drawGate(c:Canvas,w:Float,h:Float){paint.color=0xFF70482E.toInt();c.drawRect(w*.39f,h*.31f,w*.43f,h*.68f,paint);c.drawRect(w*.57f,h*.31f,w*.61f,h*.68f,paint);paint.style=Paint.Style.STROKE;paint.strokeWidth=7f;val p=Path();p.moveTo(w*.41f,h*.45f);p.quadTo(w*.5f,h*.25f,w*.59f,h*.45f);c.drawPath(p,paint);paint.style=Paint.Style.FILL}
    private fun drawPath(c:Canvas,w:Float,h:Float){paint.color=0xFFC9B99A.toInt();val p=Path();p.moveTo(w*.43f,h);p.lineTo(w*.47f,h*.58f);p.lineTo(w*.55f,h*.58f);p.lineTo(w*.67f,h);p.close();c.drawPath(p,paint)}
    private fun drawFlowers(c:Canvas,w:Float,h:Float){for(i in 0..5){paint.color=if(i%2==0)0xFFF36AA0.toInt() else 0xFFF7D65A.toInt();c.drawCircle(w*(.12f+i*.055f),h*.73f,8f,paint)}}
    private fun drawBench(c:Canvas,w:Float,h:Float){paint.color=0xFF8B5B35.toInt();c.drawRoundRect(RectF(w*.68f,h*.66f,w*.91f,h*.72f),8f,8f,paint);c.drawRect(w*.71f,h*.72f,w*.74f,h*.82f,paint);c.drawRect(w*.86f,h*.72f,w*.89f,h*.82f,paint)}
    private fun drawFountain(c:Canvas,w:Float,h:Float){paint.color=0xFFB8C7D1.toInt();c.drawOval(RectF(w*.39f,h*.69f,w*.61f,h*.83f),paint);c.drawRect(w*.485f,h*.55f,w*.515f,h*.72f,paint);paint.color=0xFF61C5ED.toInt();c.drawCircle(w*.5f,h*.55f,10f,paint)}
    private fun drawRoses(c:Canvas,w:Float,h:Float){for(i in 0..7){paint.color=0xFFD9365A.toInt();c.drawCircle(w*(.34f+i*.047f),h*.82f,7f,paint)}}
    private fun drawLanterns(c:Canvas,w:Float,h:Float){listOf(.3f,.7f).forEach{x->paint.color=0xFF29323A.toInt();c.drawRect(w*x-3,h*.48f,w*x+3,h*.72f,paint);paint.color=0xFFFFD76B.toInt();c.drawCircle(w*x,h*.47f,10f,paint)}}
    private fun drawPond(c:Canvas,w:Float,h:Float){paint.color=0xFF4BB7C9.toInt();c.drawOval(RectF(w*.06f,h*.83f,w*.34f,h*.98f),paint);paint.color=0xFF72A84A.toInt();c.drawCircle(w*.18f,h*.89f,10f,paint)}
    private fun drawRelaxCorner(c:Canvas,w:Float,h:Float){paint.color=0xFFF1E2B4.toInt();c.drawCircle(w*.84f,h*.88f,26f,paint);paint.color=0xFF5B3B2A.toInt();c.drawRect(w*.825f,h*.88f,w*.855f,h*.96f,paint)}
    private fun drawCenter(c:Canvas,w:Float,h:Float){paint.color=0xFFF5D36A.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=5f;c.drawCircle(w*.5f,h*.77f,55f,paint);paint.style=Paint.Style.FILL}
}

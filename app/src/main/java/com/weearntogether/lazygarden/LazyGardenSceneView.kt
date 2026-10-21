package com.weearntogether.lazygarden

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** Original lightweight garden scene. Completed restoration tasks appear permanently in the scene. */
class LazyGardenSceneView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var completed: Set<Int> = emptySet()

    fun setCompletedTasks(levels: Set<Int>) { completed = levels; invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        paint.color = 0xFF9ED8F5.toInt(); c.drawRect(0f, 0f, w, h * .38f, paint)
        paint.color = 0xFF3E8B4F.toInt(); c.drawRect(0f, h * .38f, w, h, paint)
        paint.color = 0xFF245C35.toInt(); c.drawCircle(w*.12f,h*.38f,h*.18f,paint); c.drawCircle(w*.88f,h*.38f,h*.19f,paint)

        if (1 in completed) drawGate(c,w,h) else drawBrokenGate(c,w,h)
        if (2 in completed) drawPath(c,w,h)
        if (3 in completed) drawFlowers(c,w,h)
        if (4 in completed) drawBench(c,w,h)
        if (5 in completed) drawFountain(c,w,h)
        if (6 in completed) drawRoses(c,w,h)
        if (7 in completed) drawLanterns(c,w,h)
        if (8 in completed) drawPond(c,w,h)
        if (9 in completed) drawRelaxCorner(c,w,h)
        if (10 in completed) drawCenter(c,w,h)
    }

    private fun drawBrokenGate(c:Canvas,w:Float,h:Float){ paint.color=0xFF6D523A.toInt(); paint.strokeWidth=8f; c.drawLine(w*.44f,h*.34f,w*.42f,h*.66f,paint); c.drawLine(w*.58f,h*.36f,w*.55f,h*.62f,paint) }
    private fun drawGate(c:Canvas,w:Float,h:Float){ paint.color=0xFF70482E.toInt(); c.drawRect(w*.39f,h*.31f,w*.43f,h*.68f,paint); c.drawRect(w*.57f,h*.31f,w*.61f,h*.68f,paint); paint.style=Paint.Style.STROKE; paint.strokeWidth=7f; val p=Path();p.moveTo(w*.41f,h*.45f);p.quadTo(w*.5f,h*.25f,w*.59f,h*.45f);c.drawPath(p,paint);paint.style=Paint.Style.FILL }
    private fun drawPath(c:Canvas,w:Float,h:Float){ paint.color=0xFFC9B99A.toInt(); val p=Path();p.moveTo(w*.43f,h);p.lineTo(w*.47f,h*.58f);p.lineTo(w*.55f,h*.58f);p.lineTo(w*.67f,h);p.close();c.drawPath(p,paint) }
    private fun drawFlowers(c:Canvas,w:Float,h:Float){ for(i in 0..5){ paint.color=if(i%2==0)0xFFF36AA0.toInt() else 0xFFF7D65A.toInt();c.drawCircle(w*(.12f+i*.055f),h*.73f,8f,paint) } }
    private fun drawBench(c:Canvas,w:Float,h:Float){ paint.color=0xFF8B5B35.toInt();c.drawRoundRect(RectF(w*.68f,h*.66f,w*.91f,h*.72f),8f,8f,paint);c.drawRect(w*.71f,h*.72f,w*.74f,h*.82f,paint);c.drawRect(w*.86f,h*.72f,w*.89f,h*.82f,paint) }
    private fun drawFountain(c:Canvas,w:Float,h:Float){ paint.color=0xFFB8C7D1.toInt();c.drawOval(RectF(w*.39f,h*.69f,w*.61f,h*.83f),paint);c.drawRect(w*.485f,h*.55f,w*.515f,h*.72f,paint);paint.color=0xFF61C5ED.toInt();c.drawCircle(w*.5f,h*.55f,10f,paint) }
    private fun drawRoses(c:Canvas,w:Float,h:Float){ for(i in 0..7){paint.color=0xFFD9365A.toInt(); val x=w*(.34f+i*.047f);c.drawCircle(x,h*.82f,7f,paint)} }
    private fun drawLanterns(c:Canvas,w:Float,h:Float){ listOf(.3f,.7f).forEach{x->paint.color=0xFF29323A.toInt();c.drawRect(w*x-3,h*.48f,w*x+3,h*.72f,paint);paint.color=0xFFFFD76B.toInt();c.drawCircle(w*x,h*.47f,10f,paint)} }
    private fun drawPond(c:Canvas,w:Float,h:Float){ paint.color=0xFF4BB7C9.toInt();c.drawOval(RectF(w*.06f,h*.83f,w*.34f,h*.98f),paint);paint.color=0xFF72A84A.toInt();c.drawCircle(w*.18f,h*.89f,10f,paint) }
    private fun drawRelaxCorner(c:Canvas,w:Float,h:Float){ paint.color=0xFFF1E2B4.toInt();c.drawCircle(w*.84f,h*.88f,26f,paint);paint.color=0xFF5B3B2A.toInt();c.drawRect(w*.825f,h*.88f,w*.855f,h*.96f,paint) }
    private fun drawCenter(c:Canvas,w:Float,h:Float){ paint.color=0xFFF5D36A.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=5f;c.drawCircle(w*.5f,h*.77f,55f,paint);paint.style=Paint.Style.FILL }
}

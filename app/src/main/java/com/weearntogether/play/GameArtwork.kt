package com.weearntogether.play

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.min

/** Compact native cover artwork so every game is visually identifiable without external assets. */
class GameArtwork(context: Context, private val game: String) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val navy = Color.rgb(7, 15, 39)
    private val purple = Color.rgb(116, 69, 235)
    private val blue = Color.rgb(32, 137, 255)
    private val gold = Color.rgb(255, 199, 66)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat(); val s = min(w, h)
        p.shader = android.graphics.LinearGradient(0f, 0f, w, h, purple, blue, android.graphics.Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(0f, 0f, w, h), s * .22f, s * .22f, p)
        p.shader = null
        p.color = Color.argb(65, 255, 255, 255)
        c.drawCircle(w * .78f, h * .18f, s * .23f, p)
        when (game) {
            "bubble" -> bubbles(c, w, h, s)
            "block" -> blocks(c, w, h, s)
            "garden" -> garden(c, w, h, s)
            "lines" -> lines(c, w, h, s)
            "stack" -> stack(c, w, h, s)
            "2048" -> tile2048(c, w, h, s)
        }
    }

    private fun bubbles(c: Canvas,w:Float,h:Float,s:Float){
        val dots=listOf(Triple(.30f,.32f,gold),Triple(.55f,.28f,Color.CYAN),Triple(.43f,.53f,Color.MAGENTA),Triple(.67f,.55f,Color.WHITE),Triple(.27f,.67f,0xff63f2a6.toInt()))
        dots.forEach{(x,y,col)->p.color=col;c.drawCircle(w*x,h*y,s*.13f,p);p.color=Color.argb(90,255,255,255);c.drawCircle(w*x-s*.035f,h*y-s*.04f,s*.035f,p)}
    }
    private fun blocks(c:Canvas,w:Float,h:Float,s:Float){
        val cells=listOf(.23f to .27f,.43f to .27f,.63f to .27f,.23f to .47f,.43f to .47f,.43f to .67f,.63f to .67f)
        cells.forEachIndexed{i,(x,y)->p.color=if(i%3==0)gold else if(i%3==1)Color.CYAN else 0xffd97cff.toInt();c.drawRoundRect(RectF(w*x,h*y,w*x+s*.17f,h*y+s*.17f),s*.035f,s*.035f,p)}
    }
    private fun garden(c:Canvas,w:Float,h:Float,s:Float){
        p.color=0xff62e28d.toInt();c.drawOval(RectF(w*.20f,h*.32f,w*.52f,h*.63f),p);c.drawOval(RectF(w*.48f,h*.23f,w*.78f,h*.55f),p)
        p.color=gold;p.strokeWidth=s*.055f;c.drawLine(w*.49f,h*.43f,w*.49f,h*.78f,p);c.drawCircle(w*.49f,h*.31f,s*.10f,p)
        p.color=navy;c.drawCircle(w*.49f,h*.31f,s*.035f,p)
    }
    private fun lines(c:Canvas,w:Float,h:Float,s:Float){
        p.strokeWidth=s*.035f;p.color=Color.argb(100,255,255,255);for(i in 1..3){c.drawLine(w*.2f,h*(.2f+i*.15f),w*.8f,h*(.2f+i*.15f),p);c.drawLine(w*(.2f+i*.15f),h*.2f,w*(.2f+i*.15f),h*.8f,p)}
        listOf(.27f to .67f,.40f to .54f,.53f to .41f,.66f to .28f).forEachIndexed{i,(x,y)->p.color=if(i%2==0)gold else Color.CYAN;c.drawCircle(w*x,h*y,s*.075f,p)}
    }
    private fun stack(c:Canvas,w:Float,h:Float,s:Float){
        val widths=listOf(.58f,.48f,.38f,.29f);widths.forEachIndexed{i,v->p.color=if(i==3)gold else if(i%2==0)Color.CYAN else 0xffd77cff.toInt();val top=h*(.70f-i*.14f);c.drawRoundRect(RectF(w*(.5f-v/2),top,w*(.5f+v/2),top+s*.10f),s*.03f,s*.03f,p)}
    }
    private fun tile2048(c:Canvas,w:Float,h:Float,s:Float){
        p.color=gold;c.drawRoundRect(RectF(w*.18f,h*.25f,w*.82f,h*.75f),s*.10f,s*.10f,p);p.color=navy;p.textAlign=Paint.Align.CENTER;p.typeface=android.graphics.Typeface.DEFAULT_BOLD;p.textSize=s*.25f;c.drawText("2048",w*.5f,h*.58f,p)
    }
}
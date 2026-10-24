package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.min

/** Rich native game cover art. No external/copyrighted game assets are used. */
class GameArtwork(context: Context, private val game: String) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val navy=Color.rgb(5,10,30); private val purple=Color.rgb(99,54,225)
    private val blue=Color.rgb(20,126,244); private val gold=Color.rgb(255,199,67)
    private fun rr(c:Canvas,l:Float,t:Float,r:Float,b:Float,rad:Float,col:Int){p.color=col;p.shader=null;c.drawRoundRect(RectF(l,t,r,b),rad,rad,p)}
    override fun onDraw(c:Canvas){
        val w=width.toFloat();val h=height.toFloat();val s=min(w,h)
        p.shader=LinearGradient(0f,0f,w,h,intArrayOf(0xff32146f.toInt(),0xff124db2.toInt(),navy),null,Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(0f,0f,w,h),s*.18f,s*.18f,p);p.shader=null
        p.color=Color.argb(45,255,255,255);c.drawCircle(w*.80f,h*.13f,s*.28f,p)
        p.color=Color.argb(35,80,190,255);c.drawCircle(w*.10f,h*.88f,s*.36f,p)
        when(game){"garden"->garden(c,w,h,s);"bubble"->bubbles(c,w,h,s);"block"->blocks(c,w,h,s);"lines"->lines(c,w,h,s);"stack"->stack(c,w,h,s);"2048"->tile2048(c,w,h,s)}
        p.style=Paint.Style.STROKE;p.strokeWidth=s*.018f;p.color=Color.argb(90,255,215,95);c.drawRoundRect(RectF(s*.025f,s*.025f,w-s*.025f,h-s*.025f),s*.16f,s*.16f,p);p.style=Paint.Style.FILL
    }
    private fun garden(c:Canvas,w:Float,h:Float,s:Float){
        // layered estate scene: sky, lawn, path, manor, fountain and flower beds
        rr(c,w*.08f,h*.56f,w*.92f,h*.88f,s*.12f,0xff20774d.toInt())
        p.color=0xffc8a36a.toInt();val path=Path().apply{moveTo(w*.43f,h*.88f);lineTo(w*.57f,h*.88f);lineTo(w*.65f,h*.50f);lineTo(w*.35f,h*.50f);close()};c.drawPath(path,p)
        rr(c,w*.28f,h*.22f,w*.72f,h*.52f,s*.025f,0xffffe3a2.toInt());rr(c,w*.34f,h*.31f,w*.43f,h*.43f,s*.012f,0xff4aa9e8.toInt());rr(c,w*.57f,h*.31f,w*.66f,h*.43f,s*.012f,0xff4aa9e8.toInt());rr(c,w*.46f,h*.35f,w*.55f,h*.52f,s*.01f,0xff79442f.toInt())
        p.color=0xff7b2b6e.toInt();val roof=Path().apply{moveTo(w*.23f,h*.24f);lineTo(w*.50f,h*.08f);lineTo(w*.77f,h*.24f);close()};c.drawPath(roof,p)
        p.color=0xff63dc85.toInt();for(x in listOf(.17f,.82f)){c.drawCircle(w*x,h*.45f,s*.14f,p);c.drawCircle(w*x,h*.34f,s*.10f,p)}
        p.color=0xff62c8ff.toInt();c.drawOval(RectF(w*.37f,h*.58f,w*.63f,h*.78f),p);p.color=0xffe7f7ff.toInt();c.drawOval(RectF(w*.43f,h*.62f,w*.57f,h*.70f),p);p.strokeWidth=s*.025f;c.drawLine(w*.5f,h*.62f,w*.5f,h*.52f,p)
        for(i in 0..5){p.color=if(i%2==0)0xffff5b8f.toInt() else gold;c.drawCircle(w*(.18f+i*.13f),h*.79f,s*.035f,p)}
    }
    private fun bubbles(c:Canvas,w:Float,h:Float,s:Float){
        val dots=listOf(Triple(.28f,.27f,0xffffcb43.toInt()),Triple(.51f,.22f,0xff4fe8ff.toInt()),Triple(.72f,.31f,0xffff68d5.toInt()),Triple(.38f,.48f,0xff73ef9c.toInt()),Triple(.62f,.53f,0xffff755e.toInt()),Triple(.27f,.68f,0xff9b78ff.toInt()),Triple(.55f,.74f,0xffffcb43.toInt()))
        dots.forEach{(x,y,col)->p.shader=RadialGradient(w*x-s*.035f,h*y-s*.045f,s*.14f,Color.WHITE,col,Shader.TileMode.CLAMP);c.drawCircle(w*x,h*y,s*.135f,p);p.shader=null;p.color=Color.argb(120,255,255,255);c.drawCircle(w*x-s*.04f,h*y-s*.045f,s*.025f,p)}
        p.color=gold;p.strokeWidth=s*.035f;c.drawLine(w*.50f,h*.94f,w*.50f,h*.83f,p);val aim=Path().apply{moveTo(w*.45f,h*.86f);lineTo(w*.5f,h*.78f);lineTo(w*.55f,h*.86f);close()};c.drawPath(aim,p)
    }
    private fun blocks(c:Canvas,w:Float,h:Float,s:Float){
        val cell=s*.13f;for(r in 0..4)for(col in 0..4){rr(c,w*.18f+col*cell,h*.18f+r*cell,w*.18f+col*cell+cell*.82f,h*.18f+r*cell+cell*.82f,s*.022f,Color.argb(35,255,255,255))}
        val filled=listOf(0 to 0,1 to 0,2 to 0,2 to 1,2 to 2,4 to 1,4 to 2,4 to 3,1 to 4,2 to 4,3 to 4)
        filled.forEachIndexed{i,(x,y)->val col=when(i%3){0->gold;1->0xff54dcff.toInt();else->0xffd875ff.toInt()};rr(c,w*.18f+x*cell,h*.18f+y*cell,w*.18f+x*cell+cell*.82f,h*.18f+y*cell+cell*.82f,s*.025f,col)}
    }
    private fun lines(c:Canvas,w:Float,h:Float,s:Float){p.strokeWidth=s*.018f;p.color=Color.argb(75,255,255,255);for(i in 0..4){c.drawLine(w*.2f,h*(.2f+i*.14f),w*.8f,h*(.2f+i*.14f),p);c.drawLine(w*(.2f+i*.15f),h*.2f,w*(.2f+i*.15f),h*.76f,p)};listOf(.27f to .68f,.39f to .56f,.51f to .44f,.63f to .32f).forEachIndexed{i,(x,y)->p.color=if(i%2==0)gold else 0xff52e8ff.toInt();c.drawCircle(w*x,h*y,s*.07f,p)}}
    private fun stack(c:Canvas,w:Float,h:Float,s:Float){listOf(.64f,.54f,.43f,.33f,.24f).forEachIndexed{i,v->rr(c,w*(.5f-v/2),h*(.75f-i*.12f),w*(.5f+v/2),h*(.75f-i*.12f)+s*.085f,s*.025f,when(i%3){0->gold;1->0xff51ddff.toInt();else->0xffd778ff.toInt()})}}
    private fun tile2048(c:Canvas,w:Float,h:Float,s:Float){rr(c,w*.17f,h*.24f,w*.83f,h*.76f,s*.10f,gold);p.color=navy;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=s*.24f;c.drawText("2048",w*.5f,h*.58f,p);p.textAlign=Paint.Align.LEFT}
}
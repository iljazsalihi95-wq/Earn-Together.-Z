package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.min

/** Original native cover artwork for the game library. */
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
        when(game){"garden"->garden(c,w,h,s);"bubble"->bubbles(c,w,h,s);"block"->blocks(c,w,h,s);"lines"->lines(c,w,h,s);"stack"->stack(c,w,h,s);"2048"->tile2048(c,w,h,s);"water"->waterSort(c,w,h,s);"knife"->knife(c,w,h,s);"arrows"->arrows(c,w,h,s);"cups"->cups(c,w,h,s)}
        p.style=Paint.Style.STROKE;p.strokeWidth=s*.018f;p.color=Color.argb(105,255,215,95);c.drawRoundRect(RectF(s*.025f,s*.025f,w-s*.025f,h-s*.025f),s*.16f,s*.16f,p);p.style=Paint.Style.FILL
    }
    private fun garden(c:Canvas,w:Float,h:Float,s:Float){rr(c,w*.08f,h*.56f,w*.92f,h*.88f,s*.12f,0xff20774d.toInt());p.color=0xffc8a36a.toInt();val path=Path().apply{moveTo(w*.43f,h*.88f);lineTo(w*.57f,h*.88f);lineTo(w*.65f,h*.50f);lineTo(w*.35f,h*.50f);close()};c.drawPath(path,p);rr(c,w*.28f,h*.22f,w*.72f,h*.52f,s*.025f,0xffffe3a2.toInt());p.color=0xff7b2b6e.toInt();val roof=Path().apply{moveTo(w*.23f,h*.24f);lineTo(w*.50f,h*.08f);lineTo(w*.77f,h*.24f);close()};c.drawPath(roof,p);p.color=0xff62c8ff.toInt();c.drawOval(RectF(w*.37f,h*.58f,w*.63f,h*.78f),p)}
    private fun glossyBubble(c:Canvas,x:Float,y:Float,r:Float,col:Int){p.shader=RadialGradient(x-r*.30f,y-r*.34f,r*1.30f,intArrayOf(Color.WHITE,col,Color.rgb((Color.red(col)*.45f).toInt(),(Color.green(col)*.45f).toInt(),(Color.blue(col)*.45f).toInt())),floatArrayOf(0f,.28f,1f),Shader.TileMode.CLAMP);c.drawCircle(x,y,r,p);p.shader=null;p.color=0xbfffffff.toInt();c.drawCircle(x-r*.31f,y-r*.33f,r*.16f,p)}
    private fun bubbles(c:Canvas,w:Float,h:Float,s:Float){val palette=intArrayOf(gold,0xff4fe8ff.toInt(),0xffff68d5.toInt(),0xff73ef9c.toInt(),0xffff755e.toInt(),0xff9b78ff.toInt());val r=s*.075f;for(row in 0..3)for(i in 0..4)glossyBubble(c,w*.22f+i*r*1.8f+(row%2)*r*.8f,h*.18f+row*r*1.7f,r,palette[(row+i)%palette.size]);glossyBubble(c,w*.5f,h*.82f,s*.085f,palette[1]);p.style=Paint.Style.STROKE;p.strokeWidth=s*.018f;p.color=Color.WHITE;c.drawLine(w*.5f,h*.72f,w*.75f,h*.48f,p);p.style=Paint.Style.FILL}
    private fun blockCell(c:Canvas,l:Float,t:Float,cell:Float,col:Int){rr(c,l,t,l+cell*.84f,t+cell*.84f,cell*.17f,col);p.color=0x55ffffff;c.drawRoundRect(RectF(l+cell*.10f,t+cell*.08f,l+cell*.68f,t+cell*.22f),cell*.07f,cell*.07f,p)}
    private fun blocks(c:Canvas,w:Float,h:Float,s:Float){val cell=s*.105f;val ox=w*.235f;val oy=h*.13f;rr(c,w*.16f,h*.08f,w*.84f,h*.73f,s*.07f,0x99200f4f.toInt());for(r in 0..5)for(col in 0..5)rr(c,ox+col*cell,oy+r*cell,ox+col*cell+cell*.84f,oy+r*cell+cell*.84f,cell*.17f,0x243f79cc);listOf(0 to 4,1 to 4,2 to 4,2 to 3,2 to 2).forEach{(x,y)->blockCell(c,ox+x*cell,oy+y*cell,cell,gold)}}
    private fun lines(c:Canvas,w:Float,h:Float,s:Float){p.strokeWidth=s*.018f;p.color=Color.argb(75,255,255,255);for(i in 0..4){c.drawLine(w*.2f,h*(.2f+i*.14f),w*.8f,h*(.2f+i*.14f),p);c.drawLine(w*(.2f+i*.15f),h*.2f,w*(.2f+i*.15f),h*.76f,p)};listOf(.27f to .68f,.39f to .56f,.51f to .44f,.63f to .32f).forEachIndexed{i,(x,y)->p.color=if(i%2==0)gold else 0xff52e8ff.toInt();c.drawCircle(w*x,h*y,s*.07f,p)}}
    private fun stack(c:Canvas,w:Float,h:Float,s:Float){listOf(.64f,.54f,.43f,.33f,.24f).forEachIndexed{i,v->rr(c,w*(.5f-v/2),h*(.75f-i*.12f),w*(.5f+v/2),h*(.75f-i*.12f)+s*.085f,s*.025f,when(i%3){0->gold;1->0xff51ddff.toInt();else->0xffd778ff.toInt()})}}
    private fun tile2048(c:Canvas,w:Float,h:Float,s:Float){rr(c,w*.17f,h*.24f,w*.83f,h*.76f,s*.10f,gold);p.color=navy;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=s*.24f;c.drawText("2048",w*.5f,h*.58f,p);p.textAlign=Paint.Align.LEFT}
    private fun waterSort(c:Canvas,w:Float,h:Float,s:Float){val cols=intArrayOf(0xff52ddff.toInt(),0xffff6fcf.toInt(),gold,0xff7cff9b.toInt());for(i in 0..3){val x=w*(.18f+i*.21f);p.style=Paint.Style.STROKE;p.strokeWidth=s*.025f;p.color=0xccffffff.toInt();c.drawRoundRect(RectF(x-s*.055f,h*.18f,x+s*.055f,h*.76f),s*.045f,s*.045f,p);p.style=Paint.Style.FILL;for(k in 0..2)rr(c,x-s*.045f,h*(.70f-k*.13f),x+s*.045f,h*(.80f-k*.13f),s*.02f,cols[(i+k)%cols.size])}}
    private fun knife(c:Canvas,w:Float,h:Float,s:Float){p.color=0xff8b542f.toInt();c.drawCircle(w*.5f,h*.47f,s*.25f,p);p.style=Paint.Style.STROKE;p.strokeWidth=s*.03f;p.color=gold;c.drawCircle(w*.5f,h*.47f,s*.21f,p);p.style=Paint.Style.FILL;for(i in 0..4){c.save();c.rotate(i*72f,w*.5f,h*.47f);rr(c,w*.485f,h*.04f,w*.515f,h*.34f,s*.012f,0xffe8f1ff.toInt());rr(c,w*.47f,h*.02f,w*.53f,h*.12f,s*.018f,0xff7451c9.toInt());c.restore()}}
    private fun arrows(c:Canvas,w:Float,h:Float,s:Float){val dirs=arrayOf(0f,90f,180f,270f,45f,-45f);dirs.forEachIndexed{i,a->val x=w*(.25f+(i%3)*.25f);val y=h*(.27f+(i/3)*.34f);c.save();c.rotate(a,x,y);p.color=if(i%2==0)gold else 0xff55ddff.toInt();rr(c,x-s*.025f,y-s*.11f,x+s*.025f,y+s*.10f,s*.018f,p.color);val path=Path().apply{moveTo(x,y-s*.18f);lineTo(x-s*.09f,y-s*.07f);lineTo(x+s*.09f,y-s*.07f);close()};c.drawPath(path,p);c.restore()}}
    private fun cups(c:Canvas,w:Float,h:Float,s:Float){p.color=gold;c.drawCircle(w*.5f,h*.72f,s*.055f,p);for(i in 0..2){val x=w*(.25f+i*.25f);p.color=if(i==1)0xff7c5cff.toInt() else 0xff3ebdf5.toInt();val path=Path().apply{moveTo(x-s*.10f,h*.32f);lineTo(x+s*.10f,h*.32f);lineTo(x+s*.075f,h*.67f);lineTo(x-s*.075f,h*.67f);close()};c.drawPath(path,p);p.color=0x66ffffff;c.drawOval(RectF(x-s*.10f,h*.28f,x+s*.10f,h*.37f),p)}}
}

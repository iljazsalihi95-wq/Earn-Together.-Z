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
        when(game){"garden"->garden(c,w,h,s);"bubble"->bubbles(c,w,h,s);"block"->blocks(c,w,h,s);"lines"->lines(c,w,h,s);"stack"->stack(c,w,h,s);"2048"->tile2048(c,w,h,s)}
        p.style=Paint.Style.STROKE;p.strokeWidth=s*.018f;p.color=Color.argb(105,255,215,95);c.drawRoundRect(RectF(s*.025f,s*.025f,w-s*.025f,h-s*.025f),s*.16f,s*.16f,p);p.style=Paint.Style.FILL
    }
    private fun garden(c:Canvas,w:Float,h:Float,s:Float){
        rr(c,w*.08f,h*.56f,w*.92f,h*.88f,s*.12f,0xff20774d.toInt())
        p.color=0xffc8a36a.toInt();val path=Path().apply{moveTo(w*.43f,h*.88f);lineTo(w*.57f,h*.88f);lineTo(w*.65f,h*.50f);lineTo(w*.35f,h*.50f);close()};c.drawPath(path,p)
        rr(c,w*.28f,h*.22f,w*.72f,h*.52f,s*.025f,0xffffe3a2.toInt());rr(c,w*.34f,h*.31f,w*.43f,h*.43f,s*.012f,0xff4aa9e8.toInt());rr(c,w*.57f,h*.31f,w*.66f,h*.43f,s*.012f,0xff4aa9e8.toInt());rr(c,w*.46f,h*.35f,w*.55f,h*.52f,s*.01f,0xff79442f.toInt())
        p.color=0xff7b2b6e.toInt();val roof=Path().apply{moveTo(w*.23f,h*.24f);lineTo(w*.50f,h*.08f);lineTo(w*.77f,h*.24f);close()};c.drawPath(roof,p)
        p.color=0xff63dc85.toInt();for(x in listOf(.17f,.82f)){c.drawCircle(w*x,h*.45f,s*.14f,p);c.drawCircle(w*x,h*.34f,s*.10f,p)}
        p.color=0xff62c8ff.toInt();c.drawOval(RectF(w*.37f,h*.58f,w*.63f,h*.78f),p);p.color=0xffe7f7ff.toInt();c.drawOval(RectF(w*.43f,h*.62f,w*.57f,h*.70f),p);p.strokeWidth=s*.025f;c.drawLine(w*.5f,h*.62f,w*.5f,h*.52f,p)
        for(i in 0..5){p.color=if(i%2==0)0xffff5b8f.toInt() else gold;c.drawCircle(w*(.18f+i*.13f),h*.79f,s*.035f,p)}
    }
    private fun glossyBubble(c:Canvas,x:Float,y:Float,r:Float,col:Int){
        p.shader=RadialGradient(x-r*.30f,y-r*.34f,r*1.30f,intArrayOf(Color.WHITE,col,Color.rgb((Color.red(col)*.45f).toInt(),(Color.green(col)*.45f).toInt(),(Color.blue(col)*.45f).toInt())),floatArrayOf(0f,.28f,1f),Shader.TileMode.CLAMP);c.drawCircle(x,y,r,p);p.shader=null
        p.color=0xbfffffff.toInt();c.drawCircle(x-r*.31f,y-r*.33f,r*.16f,p);p.style=Paint.Style.STROKE;p.strokeWidth=r*.07f;p.color=0x55ffffff;c.drawCircle(x,y,r*.91f,p);p.style=Paint.Style.FILL
    }
    private fun bubbles(c:Canvas,w:Float,h:Float,s:Float){
        // staggered arcade board with depth, shooter and ricochet aim guide
        val palette=intArrayOf(0xffffcb43.toInt(),0xff4fe8ff.toInt(),0xffff68d5.toInt(),0xff73ef9c.toInt(),0xffff755e.toInt(),0xff9b78ff.toInt())
        val rows=arrayOf(intArrayOf(0,1,2,3,4),intArrayOf(5,2,0,1),intArrayOf(3,4,5,2,0),intArrayOf(1,3,4,5))
        val r=s*.075f
        rows.forEachIndexed{row,items->items.forEachIndexed{i,v->val offset=if(row%2==0)0f else r;glossyBubble(c,w*.25f+offset+i*r*1.72f,h*.20f+row*r*1.62f,r,palette[v])}}
        p.color=0x3338b9ff;c.drawCircle(w*.50f,h*.84f,s*.13f,p);glossyBubble(c,w*.50f,h*.82f,s*.085f,palette[1])
        p.style=Paint.Style.STROKE;p.strokeWidth=s*.018f;p.strokeCap=Paint.Cap.ROUND;p.color=0xd9ffffff.toInt();val guide=Path().apply{moveTo(w*.50f,h*.72f);lineTo(w*.78f,h*.48f);lineTo(w*.66f,h*.38f)};c.drawPath(guide,p);p.style=Paint.Style.FILL
        p.color=gold;val arrow=Path().apply{moveTo(w*.46f,h*.75f);lineTo(w*.50f,h*.67f);lineTo(w*.54f,h*.75f);close()};c.drawPath(arrow,p)
        rr(c,w*.10f,h*.76f,w*.27f,h*.91f,s*.05f,0x9920124f.toInt());p.color=0xffd9ccff.toInt();p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=s*.055f;c.drawText("NEXT",w*.185f,h*.815f,p);glossyBubble(c,w*.185f,h*.865f,s*.038f,palette[5]);p.textAlign=Paint.Align.LEFT
    }
    private fun blockCell(c:Canvas,l:Float,t:Float,cell:Float,col:Int){
        rr(c,l+cell*.05f,t+cell*.05f,l+cell*.91f,t+cell*.91f,cell*.18f,Color.argb(70,0,0,0));p.shader=LinearGradient(l,t,l+cell,t+cell,intArrayOf(Color.argb(255,min(255,Color.red(col)+45),min(255,Color.green(col)+45),min(255,Color.blue(col)+45)),col),null,Shader.TileMode.CLAMP);c.drawRoundRect(RectF(l,t,l+cell*.84f,t+cell*.84f),cell*.17f,cell*.17f,p);p.shader=null;p.color=0x55ffffff;c.drawRoundRect(RectF(l+cell*.10f,t+cell*.08f,l+cell*.68f,t+cell*.22f),cell*.07f,cell*.07f,p)
    }
    private fun blocks(c:Canvas,w:Float,h:Float,s:Float){
        val cell=s*.105f;val ox=w*.235f;val oy=h*.13f
        rr(c,w*.16f,h*.08f,w*.84f,h*.73f,s*.07f,0x99200f4f.toInt())
        for(r in 0..5)for(col in 0..5)rr(c,ox+col*cell,oy+r*cell,ox+col*cell+cell*.84f,oy+r*cell+cell*.84f,cell*.17f,0x243f79cc)
        val goldCells=listOf(0 to 4,1 to 4,2 to 4,2 to 3,2 to 2);val cyanCells=listOf(4 to 0,4 to 1,4 to 2,3 to 2);val violetCells=listOf(0 to 0,1 to 0,1 to 1,2 to 1)
        goldCells.forEach{(x,y)->blockCell(c,ox+x*cell,oy+y*cell,cell,gold)};cyanCells.forEach{(x,y)->blockCell(c,ox+x*cell,oy+y*cell,cell,0xff43dfff.toInt())};violetCells.forEach{(x,y)->blockCell(c,ox+x*cell,oy+y*cell,cell,0xffc36cff.toInt())}
        // three-piece tray communicates the actual drag-and-place game
        rr(c,w*.12f,h*.78f,w*.88f,h*.94f,s*.06f,0xaa0b1739.toInt());val mini=s*.038f
        listOf(.22f to gold,.49f to 0xff43dfff.toInt(),.72f to 0xffc36cff.toInt()).forEachIndexed{i,(x,col)->when(i){0->for(k in 0..2)blockCell(c,w*x+k*mini,h*.835f,mini,col);1->for(k in 0..2)blockCell(c,w*x,h*.80f+k*mini,mini,col);else->{blockCell(c,w*x,h*.82f,mini,col);blockCell(c,w*x+mini,h*.82f,mini,col);blockCell(c,w*x+mini,h*.82f-mini,mini,col)}}}
    }
    private fun lines(c:Canvas,w:Float,h:Float,s:Float){p.strokeWidth=s*.018f;p.color=Color.argb(75,255,255,255);for(i in 0..4){c.drawLine(w*.2f,h*(.2f+i*.14f),w*.8f,h*(.2f+i*.14f),p);c.drawLine(w*(.2f+i*.15f),h*.2f,w*(.2f+i*.15f),h*.76f,p)};listOf(.27f to .68f,.39f to .56f,.51f to .44f,.63f to .32f).forEachIndexed{i,(x,y)->p.color=if(i%2==0)gold else 0xff52e8ff.toInt();c.drawCircle(w*x,h*y,s*.07f,p)}}
    private fun stack(c:Canvas,w:Float,h:Float,s:Float){listOf(.64f,.54f,.43f,.33f,.24f).forEachIndexed{i,v->rr(c,w*(.5f-v/2),h*(.75f-i*.12f),w*(.5f+v/2),h*(.75f-i*.12f)+s*.085f,s*.025f,when(i%3){0->gold;1->0xff51ddff.toInt();else->0xffd778ff.toInt()})}}
    private fun tile2048(c:Canvas,w:Float,h:Float,s:Float){rr(c,w*.17f,h*.24f,w*.83f,h*.76f,s*.10f,gold);p.color=navy;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=s*.24f;c.drawText("2048",w*.5f,h*.58f,p);p.textAlign=Paint.Align.LEFT}
}
package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin

/** Original We Earn Together artwork. No third-party game artwork or reward claims. */
class PremiumHeroView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        p.shader = LinearGradient(0f,0f,w,h,intArrayOf(0xff25105f.toInt(),0xff123c88.toInt(),0xff071127.toInt()),null,Shader.TileMode.CLAMP)
        c.drawRoundRect(0f,0f,w,h,38f,38f,p); p.shader=null

        // Original neon game-world skyline.
        for (i in 0..7) {
            val x = w * (.05f + i*.135f)
            val bh = 35f + (i%4)*22f
            p.color = if(i%2==0) 0x5535c6fa else 0x558b6cff
            c.drawRoundRect(x,h-70f-bh,x+55f,h-70f,10f,10f,p)
        }
        // Gold coin constellation.
        for(i in 0..4){
            val x=w*(.62f+i*.075f); val y=45f+sin(i*1.7).toFloat()*20f+i*15f
            glow.shader=RadialGradient(x,y,30f,intArrayOf(0x88ffc857.toInt(),0x00ffc857),null,Shader.TileMode.CLAMP);c.drawCircle(x,y,30f,glow);glow.shader=null
            p.color=0xffffc857.toInt();c.drawCircle(x,y,10f,p);p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=0xffffe29a.toInt();c.drawCircle(x,y,7f,p);p.style=Paint.Style.FILL
        }
        // Controller symbol built from primitives.
        val cx=w*.76f; val cy=h*.56f
        p.color=0xee101b3d.toInt(); c.drawRoundRect(cx-115f,cy-58f,cx+115f,cy+62f,52f,52f,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=5f;p.color=0xff9b7cff.toInt();c.drawRoundRect(cx-115f,cy-58f,cx+115f,cy+62f,52f,52f,p);p.style=Paint.Style.FILL
        p.color=Color.WHITE;c.drawRect(cx-68f,cy-12f,cx-24f,cy+2f,p);c.drawRect(cx-53f,cy-27f,cx-39f,cy+17f,p)
        p.color=0xffffc857.toInt();c.drawCircle(cx+48f,cy-10f,10f,p);p.color=0xff35c6fa.toInt();c.drawCircle(cx+73f,cy+12f,10f,p)

        p.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)
        p.color=0xffffc857.toInt();p.textSize=20f;c.drawText("WE EARN TOGETHER",28f,42f,p)
        p.color=Color.WHITE;p.textSize=37f;c.drawText("PLAY. PROGRESS.",28f,92f,p);c.drawText("LEVEL UP.",28f,134f,p)
        p.typeface=Typeface.DEFAULT;p.color=0xffc8d3f3.toInt();p.textSize=17f;c.drawText("Premium games • daily progress • challenges",28f,168f,p)
    }
}

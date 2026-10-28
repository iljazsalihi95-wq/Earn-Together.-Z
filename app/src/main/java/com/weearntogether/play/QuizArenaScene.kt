package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin

/** Original scalable 2D studio art, animated while attached and visible. */
class QuizArenaScene(context:Context):View(context) {
    var reaction=0
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private var active=false
    private fun fill(c:Canvas,l:Float,t:Float,r:Float,b:Float,color:Int,rad:Float=14f){p.shader=null;p.style=Paint.Style.FILL;p.color=color;c.drawRoundRect(l,t,r,b,rad,rad,p)}
    private fun ellipse(c:Canvas,l:Float,t:Float,r:Float,b:Float,col:Int){p.shader=null;p.color=col;c.drawOval(l,t,r,b,p)}
    override fun onAttachedToWindow(){super.onAttachedToWindow();active=true}
    override fun onDetachedFromWindow(){active=false;super.onDetachedFromWindow()}
    override fun onDraw(c:Canvas){super.onDraw(c);val save=c.save();c.scale(width/720f,height/400f);val phase=android.os.SystemClock.uptimeMillis()/700.0
        p.shader=LinearGradient(0f,0f,720f,400f,intArrayOf(0xff121b4a.toInt(),0xff39216c.toInt(),0xff07152d.toInt()),null,Shader.TileMode.CLAMP);c.drawRoundRect(0f,0f,720f,400f,28f,28f,p);p.shader=null
        for(i in 0..5){val x=65f+i*120f;val path=Path().apply{moveTo(x,0f);lineTo(x-100f+sin(phase+i).toFloat()*15,330f);lineTo(x+120f,330f);close()};p.color=0x126bdcff;c.drawPath(path,p)}
        for(i in 0..32){p.color=if(i%3==0)0xaaffd87a.toInt() else 0x557fbcff;c.drawCircle(((i*137)%700+10).toFloat(),((i*43)%260+10).toFloat(),if(i%3==0)2.5f else 1.3f,p)}
        ellipse(c,20f,292f,700f,430f,0xff143866.toInt());ellipse(c,55f,308f,665f,411f,0xff23518c.toInt());ellipse(c,86f,323f,634f,398f,0xff0b244c.toInt())
        p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=0xff59b9ee.toInt();c.drawOval(55f,308f,665f,411f,p);p.style=Paint.Style.FILL
        character(c,180f,86f,false,phase);character(c,540f,86f,true,phase)
        podium(c,86f,253f,274f,374f,0xff237fbb.toInt(),"LOJTARI");podium(c,446f,253f,634f,374f,0xff794bbd.toInt(),"PREZANTUESI")
        ellipse(c,300f,106f,420f,226f,0xffb68330.toInt());ellipse(c,306f,112f,414f,220f,0xff142455.toInt());p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=55f;p.color=0xffffd979.toInt();c.drawText(if(reaction>0)"★" else if(reaction<0)"!" else "?",360f,185f,p)
        if(reaction>0)for(i in 0..28){val x=((i*83)%700).toFloat();val y=(((phase*60+i*31)%330)).toFloat();fill(c,x,y,x+5,y+12,if(i%2==0)0xffffd979.toInt() else 0xff65e2ef.toInt(),1f)}
        c.restoreToCount(save);if(active&&windowVisibility==VISIBLE)postInvalidateDelayed(40)
    }
    private fun podium(c:Canvas,l:Float,t:Float,r:Float,b:Float,col:Int,title:String){fill(c,l+7,t+7,r+7,b+7,0xff080f29.toInt());p.shader=LinearGradient(l,t,r,b,intArrayOf(col,0xff152448.toInt()),null,Shader.TileMode.CLAMP);c.drawRoundRect(l,t,r,b,16f,16f,p);p.shader=null;fill(c,l-8,t,r+8,t+15,0xffedc16a.toInt(),5f);fill(c,l+18,t+34,r-18,b-20,0xff0c1a3c.toInt(),10f);p.color=0xffdbedff.toInt();p.textAlign=Paint.Align.CENTER;p.textSize=16f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText(title,(l+r)/2,t+66,p)}
    private fun character(c:Canvas,x:Float,y:Float,host:Boolean,phase:Double){val bob=sin(phase+if(host)1 else 0).toFloat()*2f;c.save();c.translate(x,y+bob)
        fill(c,-54f,86f,54f,183f,if(host)0xffb45dd4.toInt() else 0xff39a5e2.toInt(),27f)
        fill(c,-64f,105f,-43f,172f,0xffe5ad87.toInt(),12f);fill(c,43f,105f,64f,172f,0xffe5ad87.toInt(),12f)
        ellipse(c,-36f,5f,36f,95f,0xfff2c19c.toInt());ellipse(c,-39f,-3f,39f,45f,if(host)0xff382942.toInt() else 0xff573724.toInt());fill(c,-29f,33f,29f,60f,0xfff2c19c.toInt(),10f)
        ellipse(c,-21f,45f,-13f,54f,0xff17213e.toInt());ellipse(c,13f,45f,21f,54f,0xff17213e.toInt())
        p.color=0xff884a4a.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=3f;c.drawArc(-13f,60f,13f,77f,5f,170f,false,p);p.style=Paint.Style.FILL
        val tie=Path().apply {moveTo(0f,109f);lineTo(-8f,134f);lineTo(0f,147f);lineTo(8f,134f);close()};p.color=0xffffd979.toInt();c.drawPath(tie,p)
        if(host){fill(c,47f,126f,54f,163f,0xff122147.toInt(),3f);ellipse(c,43f,115f,59f,134f,0xffd1dbea.toInt())};c.restore()
    }
}

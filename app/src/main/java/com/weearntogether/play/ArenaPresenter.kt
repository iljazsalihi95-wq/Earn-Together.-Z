package com.weearntogether.play

import android.graphics.*
import kotlin.math.sin

/** Original resolution-independent 2D presenter rig. The jaw and lips use measured
 * speech energy; breathing, blinking, arms and facial reactions have independent joints. */
internal class ArenaPresenter {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private fun shape(c:Canvas,color:Int,block:()->Unit){p.shader=null;p.color=color;block()}
    fun draw(c:Canvas,energy:Float,speaking:Boolean,reaction:Int,time:Long){
        val e=energy.coerceIn(0f,1f)
        val breath=(sin(time/780.0)*1.2).toFloat()
        c.save();c.translate(0f,breath)
        // Back hair silhouette, torso, and blouse remain stable while the joints move.
        p.shader=LinearGradient(60f,40f,150f,160f,intArrayOf(0xff593b36.toInt(),0xff201a2a.toInt()),null,Shader.TileMode.CLAMP)
        c.drawRoundRect(51f,23f,153f,163f,43f,43f,p)
        p.shader=LinearGradient(35f,140f,170f,270f,0xff4c447f.toInt(),0xff14294b.toInt(),Shader.TileMode.CLAMP)
        val torso=Path().apply{moveTo(65f,128f);quadTo(36f,133f,27f,162f);lineTo(37f,270f);lineTo(171f,270f);lineTo(180f,163f);quadTo(164f,133f,137f,128f);close()};c.drawPath(torso,p);p.shader=null
        shape(c,0xffdba487.toInt()){c.drawRoundRect(88f,112f,119f,145f,10f,10f,p)}
        shape(c,0xfff6e5d4.toInt()){val blouse=Path().apply{moveTo(74f,132f);lineTo(102f,151f);lineTo(131f,132f);lineTo(120f,260f);lineTo(85f,260f);close()};c.drawPath(blouse,p)}
        shape(c,0xff53618b.toInt()){val left=Path().apply{moveTo(72f,130f);lineTo(96f,205f);lineTo(58f,167f);lineTo(67f,157f);lineTo(50f,151f);close()};c.drawPath(left,p);val right=Path().apply{moveTo(134f,130f);lineTo(108f,205f);lineTo(148f,168f);lineTo(138f,157f);lineTo(157f,151f);close()};c.drawPath(right,p)}
        // Gold trim, brooch, and two jacket buttons.
        shape(c,0xffffd88b.toInt()){c.drawCircle(145f,153f,4.3f,p);c.drawCircle(105f,219f,2.4f,p);c.drawCircle(105f,244f,2.4f,p)}
        val gesture=if(speaking)(sin(time/470.0)*7).toFloat() else if(reaction>0)-14f else 0f
        arm(c,41f,152f,-9f+gesture,true)
        arm(c,163f,152f,8f-gesture*.7f,false)
        // Head nods on positive feedback and tilts while explaining.
        c.save();c.rotate(if(reaction>0)(sin(time/260.0)*1.4).toFloat() else if(speaking)(sin(time/650.0)*1.8).toFloat() else 0f,102f,116f)
        p.shader=RadialGradient(91f,72f,66f,0xffffdfc4.toInt(),0xffd4977b.toInt(),Shader.TileMode.CLAMP)
        c.drawOval(65f,39f,140f,126f+e*2f,p);p.shader=null
        // Side-swept hair and highlights.
        shape(c,0xff352334.toInt()){val hair=Path().apply{moveTo(61f,77f);cubicTo(49f,17f,147f,6f,146f,76f);quadTo(124f,62f,119f,37f);quadTo(105f,67f,61f,77f);close()};c.drawPath(hair,p)}
        p.style=Paint.Style.STROKE;p.strokeWidth=2f;p.color=0xff936653.toInt();c.drawArc(66f,25f,139f,92f,196f,134f,false,p);p.style=Paint.Style.FILL
        val blink=if(time%4700L<130L).14f else 1f
        eye(c,85f,83f,blink);eye(c,120f,82f,blink)
        p.color=0xff5c3a38.toInt();p.strokeWidth=2.3f;p.style=Paint.Style.STROKE
        c.drawLine(76f,74f,92f,if(reaction<0)70f else 72f,p);c.drawLine(112f,72f,128f,if(reaction>0)70f else 73f,p)
        p.color=0xffbe806e.toInt();p.strokeWidth=1.4f;val nose=Path().apply{moveTo(103f,85f);lineTo(99f,98f);quadTo(102f,100f,106f,98f)};c.drawPath(nose,p)
        // Actual audio silence closes the mouth, irrespective of the gesture clock.
        val mouthWidth=if(reaction>0)12f else 10f
        p.style=Paint.Style.FILL;p.color=0xff773f50.toInt()
        val mouth=Path().apply{moveTo(103f-mouthWidth,109f);quadTo(103f,107f-e*3f,103f+mouthWidth,109f);quadTo(103f,111f+e*16f,103f-mouthWidth,109f);close()};c.drawPath(mouth,p)
        if(e>.13f){p.color=0xfffff3df.toInt();c.drawRoundRect(96f,109f,110f,111f+e*2f,1.5f,1.5f,p)}
        p.color=0xffb76872.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=1.8f;c.drawPath(mouth,p);p.style=Paint.Style.FILL
        p.color=0xffffd587.toInt();c.drawOval(61f,91f,65f,103f,p);c.drawOval(141f,91f,145f,103f,p)
        c.restore();c.restore()
    }
    private fun eye(c:Canvas,x:Float,y:Float,open:Float){
        p.color=0xfff9f4ed.toInt();c.drawOval(x-8f,y-3.5f*open,x+7f,y+3.5f*open,p)
        if(open>.3f){p.color=0xff466b6b.toInt();c.drawCircle(x-1.5f,y,3.4f,p);p.color=0xff191c30.toInt();c.drawCircle(x-1.8f,y,1.8f,p);p.color=Color.WHITE;c.drawCircle(x-2.5f,y-1f,.85f,p)}
        p.style=Paint.Style.STROKE;p.color=0xff553c40.toInt();p.strokeWidth=1.3f;c.drawArc(x-8f,y-4f*open,x+7f,y+4f*open,185f,170f,false,p);p.style=Paint.Style.FILL
    }
    private fun arm(c:Canvas,x:Float,y:Float,angle:Float,left:Boolean){
        c.save();c.rotate(angle,x,y)
        p.shader=LinearGradient(x-12f,y,x+12f,y+82f,0xff535383.toInt(),0xff263858.toInt(),Shader.TileMode.CLAMP)
        c.drawRoundRect(x-13f,y-9f,x+13f,y+72f,13f,13f,p);p.shader=null
        p.color=0xfff6e5d4.toInt();c.drawRoundRect(x-10f,y+63f,x+10f,y+76f,3f,3f,p)
        c.save();c.rotate(if(left)-36f else 36f,x,y+75f)
        p.color=0xffe6b093.toInt();c.drawRoundRect(x-9f,y+71f,x+9f,y+101f,8f,8f,p)
        p.color=0xfff1c6a6.toInt();c.drawOval(x-10f,y+76f,x+11f,y+92f,p)
        c.restore();c.restore()
    }
}

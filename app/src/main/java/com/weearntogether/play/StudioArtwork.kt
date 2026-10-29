package com.weearntogether.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View

object StudioAssets {
    private val images=mutableMapOf<String,Bitmap>()
    @Synchronized fun get(context:Context,key:String):Bitmap=images.getOrPut(key){if(key=="knife")knifeCover() else context.assets.open("studio/$key.webp").use{BitmapFactory.decodeStream(it)}}
    /** Uses the native game's target/blade art; this card does not depend on a missing raster. */
    private fun knifeCover():Bitmap=Bitmap.createBitmap(720,720,Bitmap.Config.ARGB_8888).also{bitmap->
        val c=Canvas(bitmap);val p=Paint(3)
        p.shader=LinearGradient(0f,0f,720f,720f,intArrayOf(0xff08162e.toInt(),0xff382074.toInt(),0xff0a3565.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,720f,720f,p)
        p.shader=RadialGradient(335f,270f,220f,intArrayOf(0xffae8aff.toInt(),0xff5132a5.toInt(),0xff161e4d.toInt()),null,Shader.TileMode.CLAMP);c.drawCircle(360f,310f,188f,p);p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=12f;p.color=0xffffc857.toInt();c.drawCircle(360f,310f,175f,p);p.strokeWidth=3f;p.color=0xffb9daff.toInt();c.drawCircle(360f,310f,146f,p);p.style=Paint.Style.FILL
        for(i in 0..3){c.save();c.rotate(i*90f,360f,310f);p.shader=LinearGradient(342f,464f,378f,580f,Color.WHITE,0xff718dbb.toInt(),Shader.TileMode.CLAMP);val blade=Path().apply{moveTo(360f,458f);lineTo(378f,530f);lineTo(367f,553f);lineTo(353f,553f);lineTo(342f,530f);close()};c.drawPath(blade,p);p.shader=null;p.color=0xffffc857.toInt();c.drawRoundRect(332f,548f,388f,562f,5f,5f,p);p.color=0xff9669e6.toInt();c.drawRoundRect(349f,561f,371f,607f,7f,7f,p);c.restore()}
        p.shader=null;p.typeface=Typeface.DEFAULT_BOLD;p.textAlign=Paint.Align.CENTER;p.color=Color.WHITE;p.textSize=72f;c.drawText("KNIFE HIT",360f,91f,p);p.textSize=30f;p.color=0xffffd675.toInt();c.drawText("PRECISION • ARCADE",360f,675f,p)
    }

}
class StudioArtwork(context:Context,private val key:String):View(context){
    private val paint=Paint(3)
    override fun onDraw(c:Canvas){super.onDraw(c);val b=StudioAssets.get(context,key);val scale=maxOf(width.toFloat()/b.width,height.toFloat()/b.height);val w=b.width*scale;val h=b.height*scale;c.drawBitmap(b,null,RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),paint)}
}

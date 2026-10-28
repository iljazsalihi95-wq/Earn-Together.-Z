package com.weearntogether.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

object StudioAssets {
    private val images=mutableMapOf<String,Bitmap>()
    @Synchronized fun get(context:Context,key:String):Bitmap=images.getOrPut(key){context.assets.open("studio/$key.webp").use{BitmapFactory.decodeStream(it)}}
}
class StudioArtwork(context:Context,private val key:String):View(context){
    private val paint=Paint(3)
    override fun onDraw(c:Canvas){super.onDraw(c);val b=StudioAssets.get(context,key);val scale=maxOf(width.toFloat()/b.width,height.toFloat()/b.height);val w=b.width*scale;val h=b.height*scale;c.drawBitmap(b,null,RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),paint)}
}

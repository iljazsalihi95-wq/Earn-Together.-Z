package com.weearntogether.play

import android.content.Context
import android.graphics.*
import kotlin.math.exp
import kotlin.math.sin

/** One photographic chair/body asset shared by both actors; only the head is personalised. */
internal class ArenaSeatedActor(context:Context) {
    private val body=StudioAssets.get(context,"seated-suit")
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val mesh=FloatArray(13*13*2)
    fun draw(canvas:Canvas,photo:Bitmap?,name:String,energy:Float,time:Long) {
        // Coordinates are authored against the 512 x 768 production sprite.
        canvas.drawBitmap(body,null,RectF(0f,0f,512f,768f),paint)
        val e=energy.coerceIn(0f,1f)
        canvas.save()
        canvas.translate(0f,(sin(time/650.0)*e*1.5).toFloat())
        val head=RectF(207f,42f,282f,170f)
        val outline=Path().apply {
            moveTo(213f,42f);quadTo(244f,29f,276f,42f)
            lineTo(282f,110f);quadTo(277f,153f,245f,170f)
            quadTo(213f,153f,207f,110f);close()
        }
        canvas.clipPath(outline)
        if(photo!=null) {
            // Crop to fill without stretching the selected face.
            val ratio=head.width()/head.height()
            val cropW=minOf(photo.width.toFloat(),photo.height*ratio)
            val cropH=cropW/ratio
            val source=Rect(((photo.width-cropW)/2).toInt(),((photo.height-cropH)/2).toInt(),((photo.width+cropW)/2).toInt(),((photo.height+cropH)/2).toInt())
            canvas.save();canvas.translate(head.left,head.top)
            canvas.scale(head.width()/photo.width,head.height()/photo.height)
            // The source crop is rendered into a mesh-sized head, driven by real audio energy.
            val cropped=source
            // Draw the original through a transformed mesh, keeping deformation local to the jaw.
            var n=0
            for(y in 0..12)for(x in 0..12){
                val px=x*photo.width/12f;val py=y*photo.height/12f
                val dx=(px-photo.width*.5f)/(photo.width*.2f)
                val dy=(py-photo.height*.76f)/(photo.height*.13f)
                mesh[n++]=(px-cropped.left)*photo.width/cropped.width()
                mesh[n++]=(py-cropped.top)*photo.height/cropped.height()+(exp((-dx*dx-dy*dy).toDouble())*e*photo.height*.025).toFloat()
            }
            canvas.drawBitmapMesh(photo,12,12,mesh,0,null,0,paint)
            canvas.restore()
        } else {
            paint.color=0xff183353.toInt();canvas.drawRect(head,paint)
            paint.color=Color.WHITE;paint.textAlign=Paint.Align.CENTER;paint.textSize=46f
            canvas.drawText(name.take(1).uppercase(),245f,118f,paint)
        }
        canvas.restore()
    }
}

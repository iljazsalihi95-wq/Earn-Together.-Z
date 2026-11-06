package com.weearntogether.play

import android.content.Context
import android.graphics.*
import kotlin.math.sin
import kotlin.math.exp

/** Photo presenter: subtle breathing/nods and a jaw mesh driven only by decoded speech. */
internal class ArenaPresenter(context:Context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val portrait:Bitmap?=runCatching {
        val source=requireNotNull(context.assets.open("studio/host-ilaz.jpg").use { BitmapFactory.decodeStream(it) })
        // Frame the supplied seated portrait, including the cap and shoulders.
        val crop=Bitmap.createBitmap(source,300,425,820,525)
        val scaled=Bitmap.createScaledBitmap(crop,492,315,true)
        if(crop!==scaled)crop.recycle()
        if(source!==scaled)source.recycle()
        scaled
    }.getOrNull()
    private val mesh=FloatArray(13*13*2)
    fun draw(c:Canvas,energy:Float,speaking:Boolean,reaction:Int,time:Long){
        val image=portrait?:return
        val e=if(speaking)energy.coerceIn(0f,1f) else 0f
        c.save()
        c.translate(4f,19f+(sin(time/1100.0)*.7).toFloat())
        c.rotate(if(speaking)(sin(time/850.0)*.65).toFloat() else if(reaction>0)(sin(time/500.0)*.8).toFloat() else 0f,100f,100f)
        p.color=0xff102445.toInt();c.drawRoundRect(0f,0f,200f,177f,16f,16f,p)
        c.save();c.clipPath(Path().apply{addRoundRect(RectF(3f,3f,197f,174f),14f,14f,Path.Direction.CW)})
        c.translate(3f,3f);c.scale(194f/image.width,171f/image.height)
        // Local deformation around the lower lip/chin; the eyes and cap stay stable.
        var n=0
        for(y in 0..12)for(x in 0..12){
            val px=x*image.width/12f;val py=y*image.height/12f
            val dx=(px-image.width*.53f)/(image.width*.12f)
            val dy=(py-image.height*.57f)/(image.height*.11f)
            mesh[n++]=px
            mesh[n++]=py+(exp((-dx*dx-dy*dy).toDouble())*e*6).toFloat()
        }
        c.drawBitmapMesh(image,12,12,mesh,0,null,0,p)
        c.restore()
        p.style=Paint.Style.STROKE;p.strokeWidth=2f;p.color=0xffffd77e.toInt();c.drawRoundRect(0f,0f,200f,177f,16f,16f,p);p.style=Paint.Style.FILL
        c.restore()
    }
}

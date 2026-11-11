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
    private val suit=Path().apply {
        moveTo(113f,184f);lineTo(185f,160f);lineTo(207f,163f);lineTo(224f,320f);lineTo(216f,362f);lineTo(148f,382f);lineTo(128f,333f);lineTo(109f,305f);lineTo(84f,348f);lineTo(41f,332f);lineTo(53f,285f);lineTo(77f,217f);close()
        moveTo(282f,162f);lineTo(348f,174f);lineTo(370f,194f);lineTo(386f,255f);lineTo(448f,321f);lineTo(477f,338f);lineTo(467f,351f);lineTo(438f,331f);lineTo(416f,307f);lineTo(398f,341f);lineTo(384f,377f);lineTo(285f,362f);lineTo(289f,299f);lineTo(271f,196f);close()
        moveTo(141f,386f);lineTo(238f,387f);lineTo(250f,450f);lineTo(226f,529f);lineTo(214f,650f);lineTo(157f,661f);lineTo(128f,603f);lineTo(118f,522f);lineTo(117f,455f);close()
        moveTo(258f,389f);lineTo(353f,384f);lineTo(392f,414f);lineTo(408f,490f);lineTo(406f,586f);lineTo(392f,656f);lineTo(330f,658f);lineTo(312f,531f);lineTo(281f,455f);close()
    }
    private val shirt=Path().apply{moveTo(210f,148f);lineTo(244f,176f);lineTo(276f,147f);lineTo(292f,192f);lineTo(286f,358f);lineTo(215f,366f);lineTo(223f,218f);close()}
    private val tie=Path().apply{moveTo(246f,176f);lineTo(262f,176f);lineTo(270f,193f);lineTo(266f,207f);lineTo(294f,377f);lineTo(278f,398f);lineTo(254f,384f);lineTo(244f,208f);lineTo(240f,193f);close()}
    init { suit.op(shirt,Path.Op.DIFFERENCE);suit.op(tie,Path.Op.DIFFERENCE);shirt.op(tie,Path.Op.DIFFERENCE) }
    private val clothingFilters=HashMap<Int,ColorMatrixColorFilter>()
    private fun recolor(canvas:Canvas,mask:Path,color:Int) {
        val filter=clothingFilters.getOrPut(color) {
            // Preserve photographic fabric shadows while changing its selected colour.
            val rgb=floatArrayOf(Color.red(color)/255f,Color.green(color)/255f,Color.blue(color)/255f)
            ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                .45f*rgb[0],1.5f*rgb[0],.15f*rgb[0],0f,0f,
                .45f*rgb[1],1.5f*rgb[1],.15f*rgb[1],0f,0f,
                .45f*rgb[2],1.5f*rgb[2],.15f*rgb[2],0f,0f,
                0f,0f,0f,1f,0f)))
        }
        val checkpoint=canvas.save();canvas.clipPath(mask);paint.colorFilter=filter
        canvas.drawBitmap(body,null,RectF(0f,0f,512f,768f),paint)
        paint.colorFilter=null;canvas.restoreToCount(checkpoint)
    }
    fun draw(canvas:Canvas,photo:Bitmap?,name:String,energy:Float,time:Long,suitColor:Int?=null,shirtColor:Int?=null,tieColor:Int?=null) {
        // Coordinates are authored against the 512 x 768 production sprite.
        canvas.drawBitmap(body,null,RectF(0f,0f,512f,768f),paint)
        suitColor?.takeUnless{it==0xff273b68.toInt()}?.let{recolor(canvas,suit,it)}
        shirtColor?.takeUnless{it==Color.WHITE}?.let{recolor(canvas,shirt,it)}
        tieColor?.takeUnless{it==0xffc99b4e.toInt()}?.let{recolor(canvas,tie,it)}
        val e=energy.coerceIn(0f,1f)
        canvas.save()
        canvas.translate(0f,(sin(time/650.0)*e*1.5).toFloat())
        val head=RectF(197f,28f,292f,170f)
        val outline=Path().apply {
            moveTo(203f,28f);quadTo(244f,15f,286f,28f)
            lineTo(292f,110f);quadTo(287f,153f,245f,170f)
            quadTo(203f,153f,197f,110f);close()
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

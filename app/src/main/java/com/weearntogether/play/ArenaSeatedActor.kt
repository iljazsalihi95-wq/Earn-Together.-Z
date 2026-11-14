package com.weearntogether.play

import android.content.Context
import android.graphics.*

/** One photographic chair/body asset shared by both actors; only the head is personalised. */
internal class ArenaSeatedActor(context:Context) {
    private val body=StudioAssets.get(context,"seated-suit")
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
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
    fun draw(canvas:Canvas,photo:Bitmap?,name:String,suitColor:Int?=null,shirtColor:Int?=null,tieColor:Int?=null) {
        // Coordinates are authored against the 512 x 768 production sprite.
        canvas.drawBitmap(body,null,RectF(0f,0f,512f,768f),paint)
        suitColor?.takeUnless{it==0xff273b68.toInt()}?.let{recolor(canvas,suit,it)}
        shirtColor?.takeUnless{it==Color.WHITE}?.let{recolor(canvas,shirt,it)}
        tieColor?.takeUnless{it==0xffc99b4e.toInt()}?.let{recolor(canvas,tie,it)}
        val checkpoint = canvas.save()
        val head = RectF(197f, 22f, 292f, 170f)
        val outline = Path().apply {
            addRoundRect(head, 46f, 44f, Path.Direction.CW)
        }
        if (photo != null) {
            canvas.clipPath(outline)
            val scale = maxOf(head.width() / photo.width, head.height() / photo.height)
            val width = photo.width * scale
            val height = photo.height * scale
            val bounds = RectF(head.centerX() - width / 2, head.centerY() - height / 2,
                head.centerX() + width / 2, head.centerY() + height / 2)
            canvas.drawBitmap(photo, null, bounds, paint)
        } else {
            paint.color = 0xff183353.toInt()
            canvas.drawPath(outline, paint)
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 46f
            canvas.drawText(name.take(1).uppercase(), head.centerX(), 118f, paint)
        }
        canvas.restoreToCount(checkpoint)
    }
}

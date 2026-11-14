package com.weearntogether.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** Photographic 2.5D host. Keep the body planted and animate only the face. */
internal class ArenaPresenter(context: Context) {
    private val portrait = StudioAssets.get(context, "arena-presenter")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bounds = RectF(0f, 0f, 384f, 512f)
    private val faceBounds = RectF(128f, 68f, 244f, 175f)
    private val face = Bitmap.createBitmap(portrait, 128, 68, 116, 107)
    private val mesh = FloatArray(9 * 9 * 2)

    fun draw(canvas: Canvas, energy: Float, time: Long) {
        canvas.drawBitmap(portrait, null, bounds, paint)
        val checkpoint = canvas.save()
        canvas.translate(faceBounds.left, faceBounds.top)
        val blink = (time % 4300L) in 0L..110L
        var cursor = 0
        for (row in 0..8) {
            for (col in 0..8) {
                val x = col * face.width / 8f
                val y = row * face.height / 8f
                val center = (1f - kotlin.math.abs(col - 4) / 4f).coerceAtLeast(0f)
                val mouth = (1f - kotlin.math.abs(y - face.height * .71f) / 15f).coerceAtLeast(0f)
                val eyes = (1f - kotlin.math.abs(y - face.height * .26f) / 15f).coerceAtLeast(0f)
                mesh[cursor++] = x + kotlin.math.sin(time / 1300.0).toFloat() * center
                mesh[cursor++] = y + center * (energy * mouth * 4f + if (blink) eyes * 2f else 0f)
            }
        }
        canvas.drawBitmapMesh(face, 8, 8, mesh, 0, null, 0, paint)
        canvas.restoreToCount(checkpoint)
    }
}

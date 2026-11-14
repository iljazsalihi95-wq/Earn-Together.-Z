package com.weearntogether.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** Photographic 2.5D host. Animate the portrait while keeping its base behind the podium. */
internal class ArenaPresenter(context: Context) {
    private val portrait = StudioAssets.get(context, "arena-presenter")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bounds = RectF(0f, 0f, 384f, 512f)
    private val mesh = FloatArray(25 * 33 * 2)
    private var speech = 0f

    fun draw(canvas: Canvas, energy: Float, time: Long) {
        speech += (energy - speech) * if (energy > speech) .65f else .38f
        val breath = kotlin.math.sin(time / 620.0).toFloat()
        val turn = kotlin.math.sin(time / 1730.0).toFloat()
        val blink = (1f - kotlin.math.abs((time % 4300L) - 70L) / 70f).coerceAtLeast(0f)
        var cursor = 0

        for (row in 0..32) {
            val y = row * bounds.height() / 32f
            val neck = (1f - y / 208f).coerceIn(0f, 1f)
            val shoulders = influence(y, 234f, 115f)
            val lips = influence(y, 145f, 23f)
            val lids = influence(y, 96f, 17f)
            for (column in 0..24) {
                val x = column * bounds.width() / 24f
                val mouth = influence(x, 186f, 56f) * lips
                val eyes = maxOf(influence(x, 163f, 28f), influence(x, 209f, 28f)) * lids
                val side = ((x - 186f) / 115f).coerceIn(-1f, 1f)
                mesh[cursor++] = x + turn * neck * 3f
                mesh[cursor++] = y - breath * shoulders * 1.55f +
                    turn * neck * side * 1.8f + speech * mouth * 4f + blink * eyes * 2.6f
            }
        }
        canvas.drawBitmapMesh(portrait, 24, 32, mesh, 0, null, 0, paint)
    }

    private fun influence(value: Float, center: Float, radius: Float): Float =
        (1f - kotlin.math.abs(value - center) / radius).coerceIn(0f, 1f)
}

package com.weearntogether.play

import android.content.Context
import android.graphics.*

/** The host's actual head is composited onto the same seated body as the player. */
internal class ArenaPresenter(context:Context) {
    private val actor=ArenaSeatedActor(context)
    private val face:Bitmap?=runCatching {
        context.assets.open("studio/host-ilaz.jpg").use { stream ->
            val source=requireNotNull(BitmapFactory.decodeStream(stream))
            // Head and cap only. Never place the source room, desk or shoulders on stage.
            val head=Bitmap.createBitmap(source,580,435,230,360)
            source.recycle()
            head
        }
    }.getOrNull()
    fun draw(c:Canvas,energy:Float,speaking:Boolean,reaction:Int,time:Long) {
        actor.draw(c,face,"PREZANTUESI",if(speaking)energy else 0f,time)
    }
}

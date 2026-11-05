package com.weearntogether.play

import android.media.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/** TTS output buffer energy, read against utterance playback elapsed time. */
internal class ArenaPcmStream {
    private var rate=24000;private var channels=1;private var encoding=AudioFormat.ENCODING_PCM_16BIT
    private var samples=0L;private var peak=.08f
    private val sums=DoubleArray(7500);private val counts=IntArray(7500)
    @Synchronized fun configure(sampleRate:Int,format:Int,channelCount:Int){rate=sampleRate.coerceAtLeast(1);channels=channelCount.coerceAtLeast(1);encoding=format;samples=0;sums.fill(0.0);counts.fill(0);peak=.08f}
    @Synchronized fun append(bytes:ByteArray){
        val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val width=when(encoding){AudioFormat.ENCODING_PCM_FLOAT->4;AudioFormat.ENCODING_PCM_8BIT->1;else->2}
        while(b.remaining()>=width){
            val v=when(encoding){AudioFormat.ENCODING_PCM_FLOAT->b.float.toDouble();AudioFormat.ENCODING_PCM_8BIT->((b.get().toInt() and 255)-128)/128.0;else->b.short/32768.0}
            val index=(samples*1000/(rate*channels)/ArenaSpeechEnvelope.FRAME_MS).toInt()
            if(index in sums.indices){sums[index]+=v*v;counts[index]++;peak=maxOf(peak,sqrt(sums[index]/counts[index]).toFloat())};samples++
        }
    }
    @Synchronized fun at(ms:Int):Float{val i=ms/ArenaSpeechEnvelope.FRAME_MS;if(i !in sums.indices||counts[i]==0)return 0f;val rms=sqrt(sums[i]/counts[i]).toFloat();return if(rms<.008f)0f else (rms/peak).coerceIn(0f,1f)}
}

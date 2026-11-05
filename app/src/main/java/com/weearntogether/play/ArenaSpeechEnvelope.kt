package com.weearntogether.play

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder
import kotlin.math.sqrt

/** PCM energy from the actual decoded audio, indexed by MediaPlayer playback time.
 * No microphone, recording permission, or synthetic random mouth movement. */
internal class ArenaSpeechEnvelope(private val levels: FloatArray) {
    fun at(positionMs: Int): Float = levels.getOrElse((positionMs / FRAME_MS).coerceAtLeast(0)) { 0f }
    companion object {
        const val FRAME_MS = 40
        fun decode(file: File): ArenaSpeechEnvelope {
            val extractor = MediaExtractor()
            var codec: MediaCodec? = null
            try {
                extractor.setDataSource(file.absolutePath)
                val track = (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                    ?: return ArenaSpeechEnvelope(FloatArray(0))
                extractor.selectTrack(track)
                val format = extractor.getTrackFormat(track)
                val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
                codec = decoder
                decoder.configure(format, null, null, 0); decoder.start()
                var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                var pcmFloat = false
                val sums = DoubleArray(7500); val counts = IntArray(7500) // at most five minutes
                var last = 0; var inputDone = false; var outputDone = false
                val info = MediaCodec.BufferInfo()
                val deadline = System.nanoTime() + 12_000_000_000L
                while (!outputDone && System.nanoTime() < deadline && !Thread.currentThread().isInterrupted) {
                    if (!inputDone) {
                        val index = decoder.dequeueInputBuffer(5000)
                        if (index >= 0) {
                            val size = extractor.readSampleData(decoder.getInputBuffer(index)!!, 0)
                            if (size < 0) { decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                            else { decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0); extractor.advance() }
                        }
                    }
                    when (val index = decoder.dequeueOutputBuffer(info, 5000)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val out = decoder.outputFormat
                            sampleRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE); channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            pcmFloat = out.containsKey(MediaFormat.KEY_PCM_ENCODING) && out.getInteger(MediaFormat.KEY_PCM_ENCODING) == android.media.AudioFormat.ENCODING_PCM_FLOAT
                        }
                        in 0..Int.MAX_VALUE -> {
                            val out = decoder.getOutputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN)
                            out.position(info.offset); out.limit(info.offset + info.size)
                            var sample = 0L
                            while (out.remaining() >= if (pcmFloat) 4 else 2) {
                                val value = if (pcmFloat) out.float.toDouble() else out.short.toDouble() / 32768.0
                                val bucket = ((info.presentationTimeUs / 1000 + sample * 1000 / (sampleRate * channels)) / FRAME_MS).toInt()
                                if (bucket in sums.indices) { sums[bucket] += value * value; counts[bucket]++; last = maxOf(last, bucket) }
                                sample++
                            }
                            outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            decoder.releaseOutputBuffer(index, false)
                        }
                    }
                }
                val rms = FloatArray(last + 1) { if (counts[it] == 0) 0f else sqrt(sums[it] / counts[it]).toFloat() }
                val peak = rms.maxOrNull()?.coerceAtLeast(.02f) ?: .02f
                return ArenaSpeechEnvelope(FloatArray(rms.size) { if (rms[it] < .008f) 0f else (rms[it] / peak).coerceIn(0f, 1f) })
            } finally { runCatching { codec?.stop() }; codec?.release(); extractor.release() }
        }
    }
}

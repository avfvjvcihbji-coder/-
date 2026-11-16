package com.example.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import kotlinx.coroutines.yield
import java.io.File

/**
 * Encodes a sequence of rendered frames on a pure #FFFFFF white background into an H.264 MP4
 * using Android's native MediaCodec + MediaMuxer pipeline.
 */
class WhiteMp4Encoder {

    suspend fun encode(
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int,
        totalFrames: Int,
        renderFrameToBitmap: (frameIndex: Int, bitmap: Bitmap) -> Unit,
        onProgress: suspend (currentFrame: Int, totalFrames: Int) -> Unit
    ) {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        val bitrate = when {
            width >= 1920 -> 8_000_000
            width >= 1280 -> 4_500_000
            else -> 2_400_000
        }

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        var inputSurface: Surface? = null
        var muxer: MediaMuxer? = null
        val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val blitPaint = Paint(Paint.FILTER_BITMAP_FLAG)

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()
            var emittedVideoFrames = 0L

            fun drain(endOfStream: Boolean) {
                if (endOfStream) {
                    encoder.signalEndOfInputStream()
                }
                while (true) {
                    val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) break
                        }
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) {
                                throw IllegalStateException("MediaCodec output format changed twice")
                            }
                            trackIndex = muxer!!.addTrack(encoder.outputFormat)
                            muxer!!.start()
                            muxerStarted = true
                        }
                        outputIndex >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outputIndex)
                                ?: throw IllegalStateException("Encoder output buffer $outputIndex was null")

                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }

                            if (bufferInfo.size != 0) {
                                if (!muxerStarted) {
                                    trackIndex = muxer!!.addTrack(encoder.outputFormat)
                                    muxer!!.start()
                                    muxerStarted = true
                                }
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                // Enforce deterministic frame-accurate PTS matching target FPS
                                bufferInfo.presentationTimeUs = emittedVideoFrames * 1_000_000L / fps
                                emittedVideoFrames++
                                muxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }

                            encoder.releaseOutputBuffer(outputIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                }
            }

            for (frame in 0 until totalFrames) {
                yield()
                renderFrameToBitmap(frame, frameBitmap)
                val canvas: Canvas = inputSurface.lockCanvas(null)
                try {
                    canvas.drawBitmap(frameBitmap, 0f, 0f, blitPaint)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }
                drain(endOfStream = false)
                onProgress(frame + 1, totalFrames)
            }

            drain(endOfStream = true)
            if (muxerStarted) {
                muxer.stop()
            }
        } finally {
            frameBitmap.recycle()
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            runCatching { inputSurface?.release() }
            runCatching { muxer?.release() }
        }
    }
}

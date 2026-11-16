package com.example.export

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure-Kotlin QuickTime (.mov) container writer using the 32-bit RGBA PNG ('png ') codec.
 * Produces a genuine MOV file with a real 8-bit Alpha Channel (depth = 32), compatible with
 * Premiere Pro, After Effects, DaVinci Resolve, Final Cut, and FFmpeg without simulated backgrounds.
 *
 * Streams frames directly to disk so memory usage stays bounded to a single frame bitmap.
 */
class QuickTimeAlphaMovWriter(
    private val outputFile: File,
    private val width: Int,
    private val height: Int,
    private val fps: Int
) {
    private val raf: RandomAccessFile
    private val chunkOffsets = mutableListOf<Long>()
    private val sampleSizes = mutableListOf<Int>()
    private var mdatHeaderPos: Long = 0L
    private var mdatDataStart: Long = 0L

    init {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()
        raf = RandomAccessFile(outputFile, "rw")
        raf.setLength(0L)
        writeFtypAtom()
        mdatHeaderPos = raf.filePointer
        // 8-byte placeholder for mdat size + 'mdat'
        raf.writeInt(0)
        raf.writeBytes("mdat")
        mdatDataStart = raf.filePointer
    }

    fun writeFrame(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream(width * height / 4)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val pngBytes = baos.toByteArray()
        val offset = raf.filePointer
        raf.write(pngBytes)
        chunkOffsets.add(offset)
        sampleSizes.add(pngBytes.size)
    }

    fun finish() {
        val mdatEnd = raf.filePointer
        val mdatSize = mdatEnd - mdatHeaderPos
        // Patch mdat size (32-bit safe up to 4GB; typical 1080p clips are < 200MB)
        raf.seek(mdatHeaderPos)
        raf.writeInt(mdatSize.toInt())
        raf.seek(mdatEnd)

        val moovBytes = buildMoovAtom()
        raf.write(moovBytes)
        raf.close()
    }

    private fun writeFtypAtom() {
        val atom = ByteArrayOutputStream()
        writeInt(atom, 20)
        atom.write("ftyp".toByteArray(Charsets.US_ASCII))
        atom.write("qt  ".toByteArray(Charsets.US_ASCII))
        writeInt(atom, 0x00000200)
        atom.write("qt  ".toByteArray(Charsets.US_ASCII))
        raf.write(atom.toByteArray())
    }

    private fun buildMoovAtom(): ByteArray {
        val timeScale = fps * 100
        val sampleDuration = 100
        val totalDuration = sampleSizes.size * sampleDuration

        val mvhd = atom("mvhd") { out ->
            writeInt(out, 0) // version + flags
            writeInt(out, 0) // creation time
            writeInt(out, 0) // mod time
            writeInt(out, timeScale)
            writeInt(out, totalDuration)
            writeInt(out, 0x00010000) // rate 1.0
            writeShort(out, 0x0100) // volume 1.0
            out.write(ByteArray(10)) // reserved
            writeIdentityMatrix(out)
            out.write(ByteArray(24)) // preview/poster/selection/current time
            writeInt(out, 2) // next track ID
        }

        val tkhd = atom("tkhd") { out ->
            writeInt(out, 0x0000000F) // version 0, flags=enabled|inMovie|inPreview|inPoster
            writeInt(out, 0)
            writeInt(out, 0)
            writeInt(out, 1) // track ID
            writeInt(out, 0) // reserved
            writeInt(out, totalDuration)
            out.write(ByteArray(8)) // reserved
            writeShort(out, 0) // layer
            writeShort(out, 0) // alt group
            writeShort(out, 0) // volume
            writeShort(out, 0) // reserved
            writeIdentityMatrix(out)
            writeInt(out, width shl 16)
            writeInt(out, height shl 16)
        }

        val mdhd = atom("mdhd") { out ->
            writeInt(out, 0)
            writeInt(out, 0)
            writeInt(out, 0)
            writeInt(out, timeScale)
            writeInt(out, totalDuration)
            writeShort(out, 0) // language
            writeShort(out, 0) // quality
        }

        val hdlrMedia = atom("hdlr") { out ->
            writeInt(out, 0)
            out.write("mhlr".toByteArray(Charsets.US_ASCII))
            out.write("vide".toByteArray(Charsets.US_ASCII))
            out.write(ByteArray(12))
            out.write(0) // empty name
        }

        val vmhd = atom("vmhd") { out ->
            writeInt(out, 0x00000001)
            writeShort(out, 0) // graphics mode
            writeShort(out, 0)
            writeShort(out, 0)
            writeShort(out, 0)
        }

        val hdlrData = atom("hdlr") { out ->
            writeInt(out, 0)
            out.write("dhlr".toByteArray(Charsets.US_ASCII))
            out.write("alis".toByteArray(Charsets.US_ASCII))
            out.write(ByteArray(12))
            out.write(0)
        }

        val urlEntry = atom("url ") { out ->
            writeInt(out, 0x00000001) // self-contained flag
        }

        val dref = atom("dref") { out ->
            writeInt(out, 0)
            writeInt(out, 1)
            out.write(urlEntry)
        }

        val dinf = atom("dinf") { out ->
            out.write(dref)
        }

        val pngSampleEntry = atom("png ") { out ->
            out.write(ByteArray(6)) // reserved
            writeShort(out, 1) // data ref index
            writeShort(out, 0) // version
            writeShort(out, 0) // revision
            out.write("appl".toByteArray(Charsets.US_ASCII)) // vendor
            writeInt(out, 0) // temporal quality
            writeInt(out, 512) // spatial quality
            writeShort(out, width)
            writeShort(out, height)
            writeInt(out, 0x00480000) // 72 dpi horiz
            writeInt(out, 0x00480000) // 72 dpi vert
            writeInt(out, 0) // data size
            writeShort(out, 1) // frame count
            val nameBytes = ByteArray(32)
            nameBytes[0] = 3
            nameBytes[1] = 'P'.code.toByte()
            nameBytes[2] = 'N'.code.toByte()
            nameBytes[3] = 'G'.code.toByte()
            out.write(nameBytes)
            writeShort(out, 32) // 32-bit RGBA with Alpha Channel
            writeShort(out, 0xFFFF) // -1 color table ID
        }

        val stsd = atom("stsd") { out ->
            writeInt(out, 0)
            writeInt(out, 1)
            out.write(pngSampleEntry)
        }

        val stts = atom("stts") { out ->
            writeInt(out, 0)
            writeInt(out, 1)
            writeInt(out, sampleSizes.size)
            writeInt(out, sampleDuration)
        }

        val stsc = atom("stsc") { out ->
            writeInt(out, 0)
            writeInt(out, 1)
            writeInt(out, 1) // first chunk
            writeInt(out, 1) // 1 sample per chunk
            writeInt(out, 1) // sample description index
        }

        val stsz = atom("stsz") { out ->
            writeInt(out, 0)
            writeInt(out, 0) // variable sample sizes
            writeInt(out, sampleSizes.size)
            for (sz in sampleSizes) writeInt(out, sz)
        }

        val stco = atom("stco") { out ->
            writeInt(out, 0)
            writeInt(out, chunkOffsets.size)
            for (off in chunkOffsets) writeInt(out, off.toInt())
        }

        val stbl = atom("stbl") { out ->
            out.write(stsd)
            out.write(stts)
            out.write(stsc)
            out.write(stsz)
            out.write(stco)
        }

        val minf = atom("minf") { out ->
            out.write(vmhd)
            out.write(hdlrData)
            out.write(dinf)
            out.write(stbl)
        }

        val mdia = atom("mdia") { out ->
            out.write(mdhd)
            out.write(hdlrMedia)
            out.write(minf)
        }

        val trak = atom("trak") { out ->
            out.write(tkhd)
            out.write(mdia)
        }

        return atom("moov") { out ->
            out.write(mvhd)
            out.write(trak)
        }
    }

    private inline fun atom(type: String, block: (ByteArrayOutputStream) -> Unit): ByteArray {
        val body = ByteArrayOutputStream()
        block(body)
        val payload = body.toByteArray()
        val full = ByteArrayOutputStream(8 + payload.size)
        writeInt(full, 8 + payload.size)
        full.write(type.toByteArray(Charsets.US_ASCII))
        full.write(payload)
        return full.toByteArray()
    }

    private fun writeIdentityMatrix(out: ByteArrayOutputStream) {
        val matrix = intArrayOf(
            0x00010000, 0, 0,
            0, 0x00010000, 0,
            0, 0, 0x40000000
        )
        for (v in matrix) writeInt(out, v)
    }

    private fun writeInt(out: ByteArrayOutputStream, value: Int) {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value)
        out.write(buf.array())
    }

    private fun writeShort(out: ByteArrayOutputStream, value: Int) {
        val buf = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(value.toShort())
        out.write(buf.array())
    }
}

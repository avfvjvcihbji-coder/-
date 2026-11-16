package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import com.example.export.QuickTimeAlphaMovWriter
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class QuickTimeAlphaMovWriterTest {

    @Test
    fun writesValidQuickTimeMovWith32BitPngAlphaAtom() {
        val tempFile = File.createTempFile("test_alpha_", ".mov")
        tempFile.deleteOnExit()

        val writer = QuickTimeAlphaMovWriter(
            outputFile = tempFile,
            width = 160,
            height = 90,
            fps = 30
        )
        val bmp = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        repeat(3) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            writer.writeFrame(bmp)
        }
        writer.finish()
        bmp.recycle()

        val bytes = tempFile.readBytes()
        assertTrue("MOV file should be > 200 bytes", bytes.size > 200)
        val ascii = bytes.toString(Charsets.ISO_8859_1)
        assertTrue("Contains ftypqt  header", ascii.contains("ftypqt  "))
        assertTrue("Contains mdat atom", ascii.contains("mdat"))
        assertTrue("Contains moov atom", ascii.contains("moov"))
        assertTrue("Contains png  codec sample entry", ascii.contains("png "))
    }
}

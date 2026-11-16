package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.ai.SampleMotionAiRepository
import com.example.motion.MotionFrameRenderer
import com.example.motion.VideoSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Motion Studio", appName)
    }

    @Test
    fun `renderer preserves pure alpha vs pure white background for shared scene`() = runTest {
        val repo = SampleMotionAiRepository()
        val scene = repo.analyzeScriptToScenes("هوش مصنوعی و آینده طراحی", VideoSettings()).getOrThrow().first()
        val renderer = MotionFrameRenderer()

        val alphaBmp = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        renderer.renderSceneFrame(
            canvas = Canvas(alphaBmp),
            width = 320,
            height = 180,
            scene = scene,
            frameIndex = 30,
            transparentBackground = true
        )
        assertEquals(0, Color.alpha(alphaBmp.getPixel(1, 1)))
        alphaBmp.recycle()

        val whiteBmp = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        renderer.renderSceneFrame(
            canvas = Canvas(whiteBmp),
            width = 320,
            height = 180,
            scene = scene,
            frameIndex = 30,
            transparentBackground = false
        )
        assertEquals(Color.WHITE, whiteBmp.getPixel(1, 1))
        whiteBmp.recycle()
    }
}

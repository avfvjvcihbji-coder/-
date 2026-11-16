package com.example

import com.example.ai.SampleMotionAiRepository
import com.example.motion.VideoSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleMotionAiRepositoryTest {

    @Test
    fun multiParagraphScriptGeneratesDistinctNonRepeatingScenes() = runTest {
        val repo = SampleMotionAiRepository()
        val script = """
            هوش مصنوعی داده‌های پیچیده را پردازش می‌کند.

            رشد سریع سیستم‌ها باعث افزایش بهره‌وری می‌شود.

            در مقایسه با روش‌های قدیمی، دقت مهندسی بیشتر است.
        """.trimIndent()

        val result = repo.analyzeScriptToScenes(script, VideoSettings())
        assertTrue(result.isSuccess)
        val scenes = result.getOrThrow()
        assertEquals(3, scenes.size)

        // Consecutive scenes must never reuse the exact same archetype or layout
        assertNotEquals(scenes[0].archetype, scenes[1].archetype)
        assertNotEquals(scenes[1].archetype, scenes[2].archetype)
        assertNotEquals(scenes[0].layoutMode, scenes[1].layoutMode)
    }
}

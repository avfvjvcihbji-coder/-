package com.example

import com.example.motion.MotionMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionMathTest {

    @Test
    fun interpolateClampsAndInterpolatesLinearly() {
        assertEquals(0f, MotionMath.interpolate(-5f, 0f, 30f, 0f, 100f), 0.001f)
        assertEquals(50f, MotionMath.interpolate(15f, 0f, 30f, 0f, 100f), 0.001f)
        assertEquals(100f, MotionMath.interpolate(50f, 0f, 30f, 0f, 100f), 0.001f)
    }

    @Test
    fun springStartsAtZeroAndSettlesNearOne() {
        assertEquals(0f, MotionMath.spring(0f, 30), 0.0001f)
        val settled = MotionMath.spring(60f, 30, damping = 14f, stiffness = 140f)
        assertTrue("Expected spring to settle near 1.0, was $settled", settled in 0.96f..1.04f)
    }

    @Test
    fun bezierIsMonotonicAtEndpoints() {
        val easing = MotionMath.bezier(0.22f, 1.0f, 0.36f, 1.0f)
        assertEquals(0f, easing(0f), 0.001f)
        assertEquals(1f, easing(1f), 0.001f)
        assertTrue(easing(0.5f) > 0.5f)
    }

    @Test
    fun lifecycleEnvelopeEntersAndExitsSmoothly() {
        assertEquals(0f, MotionMath.lifecycleEnvelope(0f, 180, 0f, 20f, 20f), 0.001f)
        assertEquals(1f, MotionMath.lifecycleEnvelope(90f, 180, 0f, 20f, 20f), 0.001f)
        assertEquals(0f, MotionMath.lifecycleEnvelope(180f, 180, 0f, 20f, 20f), 0.001f)
    }
}

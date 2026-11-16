package com.example.motion

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object MotionMath {

    fun clamp(value: Float, minVal: Float, maxVal: Float): Float =
        value.coerceIn(minVal, maxVal)

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun interpolate(
        frame: Float,
        inputStart: Float,
        inputEnd: Float,
        outputStart: Float,
        outputEnd: Float,
        easing: (Float) -> Float = { it }
    ): Float {
        if (inputEnd <= inputStart) return outputEnd
        val rawT = ((frame - inputStart) / (inputEnd - inputStart)).coerceIn(0f, 1f)
        val easedT = easing(rawT)
        return lerp(outputStart, outputEnd, easedT)
    }

    /**
     * Remotion-equivalent spring physics solver (damped harmonic oscillator).
     * Returns 0f at frame <= 0 and settles smoothly toward 1f with realistic overshoot.
     */
    fun spring(
        frame: Float,
        fps: Int,
        damping: Float = 14f,
        stiffness: Float = 140f,
        mass: Float = 1.0f,
        overshootClamping: Boolean = false
    ): Float {
        if (frame <= 0f) return 0f
        val t = frame / fps.coerceAtLeast(1).toFloat()
        val w0 = sqrt(stiffness / mass)
        val zeta = damping / (2f * sqrt(stiffness * mass))

        val result = when {
            zeta < 1f -> {
                val wd = w0 * sqrt(1f - zeta * zeta)
                val envelope = exp(-zeta * w0 * t)
                1f - envelope * (cos(wd * t) + (zeta * w0 / wd) * sin(wd * t))
            }
            zeta > 1f -> {
                val s = w0 * sqrt(zeta * zeta - 1f)
                val r1 = -w0 * zeta + s
                val r2 = -w0 * zeta - s
                val c2 = (r1) / (r1 - r2)
                val c1 = 1f - c2
                1f - (c1 * exp(r1 * t) + c2 * exp(r2 * t))
            }
            else -> {
                1f - exp(-w0 * t) * (1f + w0 * t)
            }
        }
        return if (overshootClamping) result.coerceIn(0f, 1f) else result
    }

    /**
     * Cubic Bezier easing solver matching Remotion's Easing.bezier(x1, y1, x2, y2).
     */
    fun bezier(x1: Float, y1: Float, x2: Float, y2: Float): (Float) -> Float = { x ->
        val clamped = x.coerceIn(0f, 1f)
        if (clamped == 0f || clamped == 1f) {
            clamped
        } else {
            var low = 0f
            var high = 1f
            var t = clamped
            repeat(14) {
                val bx = sampleBezier(t, x1, x2)
                if (abs(bx - clamped) < 1e-4f) return@repeat
                if (bx < clamped) low = t else high = t
                t = (low + high) * 0.5f
            }
            sampleBezier(t, y1, y2)
        }
    }

    private fun sampleBezier(t: Float, p1: Float, p2: Float): Float {
        val inv = 1f - t
        return 3f * inv * inv * t * p1 + 3f * inv * t * t * p2 + t * t * t
    }

    val easeOutCubic: (Float) -> Float = bezier(0.22f, 1.0f, 0.36f, 1.0f)
    val easeInOutCubic: (Float) -> Float = bezier(0.65f, 0.0f, 0.35f, 1.0f)
    val easeAnticipateOvershoot: (Float) -> Float = bezier(0.34f, 1.38f, 0.64f, 1.0f)
    val easeOutExpo: (Float) -> Float = { t ->
        if (t >= 1f) 1f else 1f - 2f.pow(-10f * t)
    }

    /**
     * Guarantees smooth enter -> hold -> clean controlled exit so no element pops abruptly.
     */
    fun lifecycleEnvelope(
        frame: Float,
        totalFrames: Int,
        enterStartFrame: Float,
        enterDurationFrames: Float,
        exitDurationFrames: Float
    ): Float {
        val enter = interpolate(
            frame = frame,
            inputStart = enterStartFrame,
            inputEnd = enterStartFrame + enterDurationFrames,
            outputStart = 0f,
            outputEnd = 1f,
            easing = easeOutCubic
        )
        val exitStart = (totalFrames - exitDurationFrames).coerceAtLeast(enterStartFrame + enterDurationFrames)
        val exit = interpolate(
            frame = frame,
            inputStart = exitStart,
            inputEnd = totalFrames.toFloat(),
            outputStart = 1f,
            outputEnd = 0f,
            easing = easeInOutCubic
        )
        return (enter * exit).coerceIn(0f, 1f)
    }

    fun gentlePulse(frame: Float, fps: Int, cyclesPerSec: Float = 0.6f): Float {
        val t = frame / fps.coerceAtLeast(1).toFloat()
        return (0.5f + 0.5f * sin(2f * PI.toFloat() * cyclesPerSec * t))
    }
}

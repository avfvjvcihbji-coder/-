package com.example.motion

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class MotionFrameRenderer(
    private val primaryTypeface: Typeface = Typeface.SANS_SERIF,
    private val monoTypeface: Typeface = Typeface.MONOSPACE
) {
    companion object {
        const val STAGE_WIDTH = 1920f
        const val STAGE_HEIGHT = 1080f
        const val SAFE_MARGIN_X = 192f // 10% horizontal safe margin
        const val SAFE_MARGIN_Y = 108f // 10% vertical safe margin
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Renders a full multi-scene MotionProject at [globalFrame] into [canvas] sized [width]x[height].
     */
    fun renderProjectFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        project: MotionProject,
        globalFrame: Int,
        transparentBackground: Boolean,
        customPreviewBgColor: Int? = null
    ) {
        if (project.scenes.isEmpty()) {
            clearStage(canvas, transparentBackground, customPreviewBgColor)
            return
        }
        var cursor = 0
        for (scene in project.scenes) {
            val sceneFrames = scene.totalFrames
            if (globalFrame < cursor + sceneFrames || scene == project.scenes.last()) {
                val localFrame = (globalFrame - cursor).coerceIn(0, sceneFrames - 1)
                renderSceneFrame(
                    canvas = canvas,
                    width = width,
                    height = height,
                    scene = scene,
                    frameIndex = localFrame,
                    animationSpeed = project.settings.animationSpeed,
                    transparentBackground = transparentBackground,
                    customPreviewBgColor = customPreviewBgColor
                )
                return
            }
            cursor += sceneFrames
        }
    }

    /**
     * Renders a single [scene] deterministically at [frameIndex] in canonical 1920x1080 coordinates.
     * Both Preview Player and Dual-Export (White MP4 & Transparent Alpha MOV/PNG) share this exact method.
     */
    fun renderSceneFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        scene: MotionScene,
        frameIndex: Int,
        animationSpeed: Float = 1.0f,
        transparentBackground: Boolean,
        customPreviewBgColor: Int? = null
    ) {
        clearStage(canvas, transparentBackground, customPreviewBgColor)

        val totalFrames = scene.totalFrames
        val effectiveFrame = (frameIndex.toFloat() * animationSpeed).coerceIn(0f, totalFrames.toFloat())
        val fps = scene.fps.coerceAtLeast(1)
        val palette = scene.palette

        val saveRoot = canvas.save()
        canvas.scale(width / STAGE_WIDTH, height / STAGE_HEIGHT)

        // Subtle studio grid only when opaque background is active (never baked into Alpha export)
        if (!transparentBackground && customPreviewBgColor == null) {
            drawSubtleSwissGrid(canvas, palette)
        }

        // Compute transition envelope (smooth enter & exit)
        val transitionInFrames = (fps * 0.65f).coerceAtLeast(10f)
        val transitionOutFrames = (fps * 0.65f).coerceAtLeast(10f)
        val sceneEnvelope = MotionMath.lifecycleEnvelope(
            frame = effectiveFrame,
            totalFrames = totalFrames,
            enterStartFrame = 0f,
            enterDurationFrames = transitionInFrames,
            exitDurationFrames = transitionOutFrames
        )

        // Compute Camera Rig (Zoom + Pan + Transition push)
        val camProgress = MotionMath.interpolate(
            frame = effectiveFrame,
            inputStart = 0f,
            inputEnd = totalFrames.toFloat(),
            outputStart = 0f,
            outputEnd = 1f,
            easing = MotionMath.easeInOutCubic
        )
        val zoomBase = MotionMath.lerp(scene.cameraRig.zoomStart, scene.cameraRig.zoomEnd, camProgress)
        val transitionScaleOffset = when (scene.transitionIn) {
            TransitionType.DEPTH_ZOOM_DISSOLVE -> (1f - sceneEnvelope) * 0.08f
            TransitionType.IRIS_REVEAL -> -(1f - sceneEnvelope) * 0.04f
            else -> 0f
        }
        val camZoom = (zoomBase + transitionScaleOffset).coerceIn(0.85f, 1.25f)
        val camPanX = MotionMath.lerp(scene.cameraRig.panXStart, scene.cameraRig.panXEnd, camProgress)
        val camPanY = MotionMath.lerp(scene.cameraRig.panYStart, scene.cameraRig.panYEnd, camProgress)

        val slideOffsetX = if (scene.transitionIn == TransitionType.KINETIC_SLIDE) {
            val enterShift = MotionMath.interpolate(
                effectiveFrame, 0f, transitionInFrames, 90f, 0f, MotionMath.easeOutCubic
            )
            val exitShift = MotionMath.interpolate(
                effectiveFrame, totalFrames - transitionOutFrames, totalFrames.toFloat(), 0f, -90f, MotionMath.easeInOutCubic
            )
            enterShift + exitShift
        } else 0f

        // Draw each element sorted by depth layer for authentic parallax & occlusion
        val sortedElements = scene.elements.sortedBy { it.depthLayer }
        for (element in sortedElements) {
            val delayFrames = element.enterDelayRatio * totalFrames * 0.45f
            val elemEnvelope = MotionMath.lifecycleEnvelope(
                frame = effectiveFrame,
                totalFrames = totalFrames,
                enterStartFrame = delayFrames,
                enterDurationFrames = (fps * 0.7f).coerceAtLeast(12f),
                exitDurationFrames = transitionOutFrames
            )
            if (elemEnvelope <= 0.005f) continue

            val elemSpring = MotionMath.spring(
                frame = (effectiveFrame - delayFrames).coerceAtLeast(0f),
                fps = fps,
                damping = 14f,
                stiffness = 135f
            )

            val parallax = element.depthLayer * scene.cameraRig.parallaxStrength
            val layerSave = canvas.save()
            canvas.translate(
                STAGE_WIDTH * 0.5f + camPanX * parallax + slideOffsetX * parallax,
                STAGE_HEIGHT * 0.5f + camPanY * parallax
            )
            canvas.scale(camZoom, camZoom)
            canvas.translate(-STAGE_WIDTH * 0.5f, -STAGE_HEIGHT * 0.5f)

            when (element) {
                is MotionElementSpec.FloatingParticlesSpec ->
                    drawFloatingParticles(canvas, element, palette, effectiveFrame, fps, elemEnvelope)
                is MotionElementSpec.BezierFlowSpec ->
                    drawBezierFlow(canvas, element, palette, effectiveFrame, fps, elemSpring, elemEnvelope)
                is MotionElementSpec.PrecisionHudSpec ->
                    drawPrecisionHud(canvas, element, palette, effectiveFrame, fps, elemSpring, elemEnvelope)
                is MotionElementSpec.NeuralGraphSpec ->
                    drawNeuralGraph(canvas, element, palette, effectiveFrame, fps, elemSpring, elemEnvelope)
                is MotionElementSpec.AscendingColumnsSpec ->
                    drawAscendingColumns(canvas, element, palette, effectiveFrame, fps, elemSpring, elemEnvelope)
                is MotionElementSpec.VelocityVectorsSpec ->
                    drawVelocityVectors(canvas, element, palette, effectiveFrame, fps, elemSpring, elemEnvelope)
                is MotionElementSpec.DualBalanceSpec ->
                    drawDualBalance(canvas, element, palette, effectiveFrame, fps, elemSpring, elemEnvelope)
                is MotionElementSpec.OrbitalPrism3DSpec ->
                    drawOrbitalPrism3D(canvas, element, palette, effectiveFrame, fps, elemSpring, elemEnvelope)
                is MotionElementSpec.GlassFocusCardSpec ->
                    drawGlassFocusCard(canvas, element, palette, elemSpring, elemEnvelope, transparentBackground)
                is MotionElementSpec.LightSweepSpec ->
                    drawLightSweep(canvas, element, palette, effectiveFrame, totalFrames, elemEnvelope)
            }
            canvas.restoreToCount(layerSave)
        }

        // Transition Iris / Light Sweep Wipe accent
        if (scene.transitionIn == TransitionType.IRIS_REVEAL && sceneEnvelope < 0.99f) {
            drawIrisRingAccent(canvas, palette, sceneEnvelope)
        }

        canvas.restoreToCount(saveRoot)
    }

    private fun clearStage(
        canvas: Canvas,
        transparentBackground: Boolean,
        customPreviewBgColor: Int?
    ) {
        if (transparentBackground) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        } else {
            canvas.drawColor(customPreviewBgColor ?: Color.WHITE)
        }
    }

    private fun drawSubtleSwissGrid(canvas: Canvas, palette: ScenePalette) {
        strokePaint.color = withAlpha(palette.inkColor, 0.045f)
        strokePaint.strokeWidth = 1.2f
        strokePaint.pathEffect = null
        val step = 120f
        var x = step
        while (x < STAGE_WIDTH) {
            canvas.drawLine(x, 0f, x, STAGE_HEIGHT, strokePaint)
            x += step
        }
        var y = step
        while (y < STAGE_HEIGHT) {
            canvas.drawLine(0f, y, STAGE_WIDTH, y, strokePaint)
            y += step
        }
        // Subtle intersection dots
        fillPaint.color = withAlpha(palette.primaryColor, 0.12f)
        x = step
        while (x < STAGE_WIDTH) {
            y = step
            while (y < STAGE_HEIGHT) {
                canvas.drawCircle(x, y, 2.2f, fillPaint)
                y += step
            }
            x += step
        }
    }

    private fun drawFloatingParticles(
        canvas: Canvas,
        spec: MotionElementSpec.FloatingParticlesSpec,
        palette: ScenePalette,
        frame: Float,
        fps: Int,
        envelope: Float
    ) {
        val timeSec = frame / fps.toFloat()
        for (i in 0 until spec.count) {
            val h1 = hashFloat(spec.seed * 31 + i * 17)
            val h2 = hashFloat(spec.seed * 53 + i * 29)
            val h3 = hashFloat(spec.seed * 71 + i * 43)

            val baseX = SAFE_MARGIN_X * 0.6f + h1 * (STAGE_WIDTH - SAFE_MARGIN_X * 1.2f)
            val baseY = SAFE_MARGIN_Y * 0.6f + h2 * (STAGE_HEIGHT - SAFE_MARGIN_Y * 1.2f)
            val driftX = sin(timeSec * (0.6f + h3 * 0.8f) + i) * (24f + h1 * 18f)
            val driftY = cos(timeSec * (0.5f + h2 * 0.7f) + i * 1.3f) * (20f + h2 * 16f)
            val radius = 3.5f + h3 * 6.5f

            val color = when (i % 3) {
                0 -> palette.primaryColor
                1 -> palette.secondaryColor
                else -> palette.accentColor
            }
            val alpha = (0.14f + h3 * 0.24f) * envelope
            fillPaint.shader = null
            fillPaint.color = withAlpha(color, alpha)
            canvas.drawCircle(baseX + driftX, baseY + driftY, radius, fillPaint)

            if (i % 4 == 0) {
                strokePaint.color = withAlpha(color, alpha * 0.7f)
                strokePaint.strokeWidth = 1.5f
                strokePaint.pathEffect = null
                canvas.drawCircle(baseX + driftX, baseY + driftY, radius * 2.2f, strokePaint)
            }
        }
    }

    private fun drawBezierFlow(
        canvas: Canvas,
        spec: MotionElementSpec.BezierFlowSpec,
        palette: ScenePalette,
        frame: Float,
        fps: Int,
        springVal: Float,
        envelope: Float
    ) {
        val timeSec = frame / fps.toFloat()
        val drawProgress = (springVal * 0.92f + 0.08f).coerceIn(0f, 1f)

        for (layer in 0 until spec.layerCount) {
            val phaseOffset = layer * 0.55f
            val path = Path()
            path.moveTo(spec.startX, spec.startY)
            val segments = 32
            val dx = (spec.endX - spec.startX) / segments
            val dy = (spec.endY - spec.startY) / segments
            for (s in 1..segments) {
                val t = s / segments.toFloat()
                val px = spec.startX + dx * s
                val wave = sin(t * PI.toFloat() * spec.waveCount + timeSec * 2.4f + phaseOffset) *
                    spec.amplitude * sin(t * PI.toFloat())
                val py = spec.startY + dy * s + wave
                path.lineTo(px, py)
            }

            val measure = PathMeasure(path, false)
            val partialPath = Path()
            measure.getSegment(0f, measure.length * drawProgress, partialPath, true)

            val color = when (layer % 3) {
                0 -> palette.primaryColor
                1 -> palette.secondaryColor
                else -> palette.accentColor
            }

            // Soft outer glow stroke
            strokePaint.pathEffect = null
            strokePaint.color = withAlpha(color, 0.14f * envelope)
            strokePaint.strokeWidth = 14f - layer * 2.5f
            canvas.drawPath(partialPath, strokePaint)

            // Crisp core line
            strokePaint.color = withAlpha(color, (0.85f - layer * 0.18f) * envelope)
            strokePaint.strokeWidth = 4.2f - layer * 0.8f
            canvas.drawPath(partialPath, strokePaint)

            // Glowing leading orb
            val headPos = FloatArray(2)
            if (measure.getPosTan(measure.length * drawProgress, headPos, null)) {
                drawGlowOrb(canvas, headPos[0], headPos[1], 14f - layer * 2f, color, envelope)
            }
        }
    }

    private fun drawPrecisionHud(
        canvas: Canvas,
        spec: MotionElementSpec.PrecisionHudSpec,
        palette: ScenePalette,
        frame: Float,
        fps: Int,
        springVal: Float,
        envelope: Float
    ) {
        val timeSec = frame / fps.toFloat()
        val scale = MotionMath.lerp(0.75f, 1.0f, springVal)

        // Soft radial halo behind HUD
        drawGlowOrb(canvas, spec.centerX, spec.centerY, spec.outerRadius * 0.85f, palette.secondaryColor, 0.22f * envelope)

        for (r in 0 until spec.ringCount) {
            val radius = spec.outerRadius * (1f - r * 0.22f) * scale
            val rect = RectF(
                spec.centerX - radius,
                spec.centerY - radius,
                spec.centerX + radius,
                spec.centerY + radius
            )
            val dir = if (r % 2 == 0) 1f else -1f
            val startAngle = timeSec * (26f + r * 14f) * dir
            val sweepAngle = (240f - r * 35f) * springVal.coerceIn(0.1f, 1f)

            val color = if (r % 2 == 0) palette.primaryColor else palette.secondaryColor
            strokePaint.color = withAlpha(color, (0.82f - r * 0.14f) * envelope)
            strokePaint.strokeWidth = 5.5f - r * 1.1f
            strokePaint.pathEffect = if (r == 1) DashPathEffect(floatArrayOf(18f, 12f), 0f) else null
            canvas.drawArc(rect, startAngle, sweepAngle, false, strokePaint)

            // Secondary counter-arc
            strokePaint.color = withAlpha(palette.accentColor, 0.35f * envelope)
            strokePaint.strokeWidth = 2.5f
            strokePaint.pathEffect = null
            canvas.drawArc(rect, startAngle + sweepAngle + 25f, 45f * springVal, false, strokePaint)
        }

        // Calibration radial ticks
        val tickCount = 24
        val innerTickR = spec.outerRadius * 1.06f * scale
        val outerTickR = spec.outerRadius * 1.13f * scale
        strokePaint.pathEffect = null
        for (i in 0 until tickCount) {
            val angle = Math.toRadians((i * (360.0 / tickCount)) + timeSec * 8.0)
            val active = (i % 3 == 0)
            strokePaint.color = withAlpha(
                if (active) palette.primaryColor else palette.inkColor,
                (if (active) 0.65f else 0.18f) * envelope
            )
            strokePaint.strokeWidth = if (active) 3f else 1.5f
            val x1 = spec.centerX + cos(angle).toFloat() * innerTickR
            val y1 = spec.centerY + sin(angle).toFloat() * innerTickR
            val x2 = spec.centerX + cos(angle).toFloat() * outerTickR
            val y2 = spec.centerY + sin(angle).toFloat() * outerTickR
            canvas.drawLine(x1, y1, x2, y2, strokePaint)
        }
    }

    private fun drawNeuralGraph(
        canvas: Canvas,
        spec: MotionElementSpec.NeuralGraphSpec,
        palette: ScenePalette,
        frame: Float,
        fps: Int,
        springVal: Float,
        envelope: Float
    ) {
        val timeSec = frame / fps.toFloat()
        val nodes = ArrayList<PointF>(spec.nodeCount)
        for (i in 0 until spec.nodeCount) {
            val angle = (i.toFloat() / spec.nodeCount) * 2f * PI.toFloat() + spec.seed * 0.4f
            val ringFactor = if (i == 0) 0f else if (i % 2 == 0) 0.95f else 0.62f
            val floatX = sin(timeSec * 1.4f + i * 1.1f) * 14f
            val floatY = cos(timeSec * 1.1f + i * 0.9f) * 14f
            val nx = spec.centerX + cos(angle) * spec.radiusX * ringFactor * springVal + floatX
            val ny = spec.centerY + sin(angle) * spec.radiusY * ringFactor * springVal + floatY
            nodes.add(PointF(nx, ny))
        }

        // Curved synaptic connections + animated data packets
        strokePaint.pathEffect = null
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val a = nodes[i]
                val b = nodes[j]
                val dist = hypot(b.x - a.x, b.y - a.y)
                if (dist < spec.radiusX * 1.25f) {
                    val midX = (a.x + b.x) * 0.5f + (if ((i + j) % 2 == 0) 26f else -26f)
                    val midY = (a.y + b.y) * 0.5f - 22f
                    val path = Path().apply {
                        moveTo(a.x, a.y)
                        quadTo(midX, midY, b.x, b.y)
                    }
                    val measure = PathMeasure(path, false)
                    val partial = Path()
                    val edgeReveal = MotionMath.clamp(springVal * 1.15f - (i * 0.04f), 0f, 1f)
                    measure.getSegment(0f, measure.length * edgeReveal, partial, true)

                    strokePaint.color = withAlpha(palette.primaryColor, 0.28f * envelope)
                    strokePaint.strokeWidth = 2.6f
                    canvas.drawPath(partial, strokePaint)

                    // Signal pulse traveling along synapse
                    if (edgeReveal > 0.7f) {
                        val pulseT = ((timeSec * 0.75f + (i * 0.23f + j * 0.17f)) % 1f)
                        val pos = FloatArray(2)
                        if (measure.getPosTan(measure.length * pulseT, pos, null)) {
                            drawGlowOrb(canvas, pos[0], pos[1], 7.5f, palette.secondaryColor, envelope)
                        }
                    }
                }
            }
        }

        // Draw nodes with frosted rings
        for (i in nodes.indices) {
            val p = nodes[i]
            val isHub = i == 0
            val r = if (isHub) 28f else 15f
            val nodeColor = if (isHub) palette.accentColor else if (i % 2 == 0) palette.primaryColor else palette.secondaryColor
            drawGlowOrb(canvas, p.x, p.y, r * 2.1f, nodeColor, 0.35f * envelope)

            fillPaint.shader = null
            fillPaint.color = withAlpha(Color.WHITE, 0.94f * envelope)
            canvas.drawCircle(p.x, p.y, r, fillPaint)

            strokePaint.color = withAlpha(nodeColor, 0.95f * envelope)
            strokePaint.strokeWidth = if (isHub) 5f else 3.5f
            canvas.drawCircle(p.x, p.y, r, strokePaint)

            fillPaint.color = withAlpha(nodeColor, 0.9f * envelope)
            canvas.drawCircle(p.x, p.y, r * 0.42f, fillPaint)
        }
    }

    private fun drawAscendingColumns(
        canvas: Canvas,
        spec: MotionElementSpec.AscendingColumnsSpec,
        palette: ScenePalette,
        frame: Float,
        fps: Int,
        springVal: Float,
        envelope: Float
    ) {
        val gap = 28f
        val colWidth = ((spec.totalWidth - gap * (spec.columnCount - 1)) / spec.columnCount).coerceAtLeast(32f)
        val topPoints = ArrayList<PointF>()

        // Baseline axis
        strokePaint.pathEffect = null
        strokePaint.color = withAlpha(palette.inkColor, 0.16f * envelope)
        strokePaint.strokeWidth = 2.5f
        canvas.drawLine(spec.originX - 24f, spec.baselineY, spec.originX + spec.totalWidth + 24f, spec.baselineY, strokePaint)

        for (i in 0 until spec.columnCount) {
            val stagger = MotionMath.clamp(springVal * 1.25f - i * 0.08f, 0f, 1f)
            val ratio = (0.32f + (i.toFloat() / (spec.columnCount - 1).coerceAtLeast(1)) * 0.68f)
            val targetH = spec.maxHeight * ratio * stagger
            val left = spec.originX + i * (colWidth + gap)
            val top = spec.baselineY - targetH
            val rect = RectF(left, top, left + colWidth, spec.baselineY)

            val topColor = if (i == spec.columnCount - 1) palette.accentColor else palette.secondaryColor
            val bottomColor = palette.primaryColor

            fillPaint.shader = LinearGradient(
                left, top, left, spec.baselineY,
                withAlpha(topColor, 0.92f * envelope),
                withAlpha(bottomColor, 0.72f * envelope),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rect, 18f, 18f, fillPaint)
            fillPaint.shader = null

            // Specular highlight rim
            strokePaint.color = withAlpha(Color.WHITE, 0.65f * envelope)
            strokePaint.strokeWidth = 2.2f
            canvas.drawRoundRect(rect, 18f, 18f, strokePaint)

            topPoints.add(PointF(left + colWidth * 0.5f, top - 28f))
        }

        // Smooth trend trajectory curve above columns
        if (topPoints.size >= 2) {
            val trendPath = Path().apply {
                moveTo(topPoints.first().x, topPoints.first().y)
                for (i in 1 until topPoints.size) {
                    val prev = topPoints[i - 1]
                    val curr = topPoints[i]
                    val cx = (prev.x + curr.x) * 0.5f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }
            val measure = PathMeasure(trendPath, false)
            val drawPath = Path()
            measure.getSegment(0f, measure.length * springVal.coerceIn(0f, 1f), drawPath, true)
            strokePaint.color = withAlpha(palette.accentColor, 0.9f * envelope)
            strokePaint.strokeWidth = 4.5f
            canvas.drawPath(drawPath, strokePaint)

            val endPos = FloatArray(2)
            if (measure.getPosTan(measure.length * springVal.coerceIn(0f, 1f), endPos, null)) {
                drawGlowOrb(canvas, endPos[0], endPos[1], 14f, palette.accentColor, envelope)
            }
        }
    }

    private fun drawVelocityVectors(
        canvas: Canvas,
        spec: MotionElementSpec.VelocityVectorsSpec,
        palette: ScenePalette,
        frame: Float,
        fps: Int,
        springVal: Float,
        envelope: Float
    ) {
        val timeSec = frame / fps.toFloat()
        val save = canvas.save()
        canvas.rotate(spec.directionAngleDeg, spec.originX + spec.spanWidth * 0.5f, spec.centerY)

        for (i in 0 until spec.streakCount) {
            val offsetRatio = (i - spec.streakCount * 0.5f) / spec.streakCount.toFloat()
            val y = spec.centerY + offsetRatio * 260f
            val cycle = ((timeSec * (1.2f + (i % 3) * 0.35f) + i * 0.21f) % 1f)
            val length = (160f + (i % 4) * 95f) * springVal
            val startX = spec.originX + cycle * (spec.spanWidth - length)
            val endX = startX + length

            val color = when (i % 3) {
                0 -> palette.primaryColor
                1 -> palette.secondaryColor
                else -> palette.accentColor
            }

            strokePaint.shader = LinearGradient(
                startX, y, endX, y,
                withAlpha(color, 0f),
                withAlpha(color, 0.92f * envelope),
                Shader.TileMode.CLAMP
            )
            strokePaint.strokeWidth = 10f - (i % 3) * 2.5f
            strokePaint.pathEffect = null
            canvas.drawLine(startX, y, endX, y, strokePaint)
            strokePaint.shader = null

            drawGlowOrb(canvas, endX, y, 10f, color, 0.9f * envelope)
        }
        canvas.restoreToCount(save)
    }

    private fun drawDualBalance(
        canvas: Canvas,
        spec: MotionElementSpec.DualBalanceSpec,
        palette: ScenePalette,
        frame: Float,
        fps: Int,
        springVal: Float,
        envelope: Float
    ) {
        val timeSec = frame / fps.toFloat()
        val halfSpread = spec.spreadWidth * 0.5f * springVal
        val bobLeft = sin(timeSec * 2.0f) * 18f
        val bobRight = -bobLeft

        val leftX = spec.centerX - halfSpread
        val leftY = spec.centerY + bobLeft
        val rightX = spec.centerX + halfSpread
        val rightY = spec.centerY + bobRight

        // Dynamic tension bridge
        val bridgePath = Path().apply {
            moveTo(leftX, leftY)
            cubicTo(
                spec.centerX - halfSpread * 0.3f, leftY - 90f,
                spec.centerX + halfSpread * 0.3f, rightY + 90f,
                rightX, rightY
            )
        }
        strokePaint.pathEffect = null
        strokePaint.shader = LinearGradient(
            leftX, leftY, rightX, rightY,
            withAlpha(palette.primaryColor, 0.9f * envelope),
            withAlpha(palette.accentColor, 0.9f * envelope),
            Shader.TileMode.CLAMP
        )
        strokePaint.strokeWidth = 5f
        canvas.drawPath(bridgePath, strokePaint)
        strokePaint.shader = null

        // Left Element (Structured Square/Rounded Tile)
        drawGlowOrb(canvas, leftX, leftY, 130f, palette.primaryColor, 0.28f * envelope)
        val leftSize = 110f * springVal
        val leftRect = RectF(leftX - leftSize, leftY - leftSize, leftX + leftSize, leftY + leftSize)
        fillPaint.color = withAlpha(palette.primaryColor, 0.16f * envelope)
        canvas.drawRoundRect(leftRect, 34f, 34f, fillPaint)
        strokePaint.color = withAlpha(palette.primaryColor, 0.95f * envelope)
        strokePaint.strokeWidth = 4f
        canvas.drawRoundRect(leftRect, 34f, 34f, strokePaint)

        // Right Element (Organic Sphere)
        drawGlowOrb(canvas, rightX, rightY, 130f, palette.accentColor, 0.28f * envelope)
        val rightR = 108f * springVal
        fillPaint.color = withAlpha(palette.accentColor, 0.16f * envelope)
        canvas.drawCircle(rightX, rightY, rightR, fillPaint)
        strokePaint.color = withAlpha(palette.accentColor, 0.95f * envelope)
        strokePaint.strokeWidth = 4f
        canvas.drawCircle(rightX, rightY, rightR, strokePaint)

        // Fulcrum center diamond
        val fulcrumR = 26f * springVal
        val diamond = Path().apply {
            moveTo(spec.centerX, spec.centerY - fulcrumR)
            lineTo(spec.centerX + fulcrumR, spec.centerY)
            lineTo(spec.centerX, spec.centerY + fulcrumR)
            lineTo(spec.centerX - fulcrumR, spec.centerY)
            close()
        }
        fillPaint.color = withAlpha(palette.secondaryColor, 0.95f * envelope)
        canvas.drawPath(diamond, fillPaint)

        // Labels inside shapes
        textPaint.typeface = primaryTypeface
        textPaint.textSize = 30f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = withAlpha(palette.inkColor, envelope)
        canvas.drawText(spec.leftLabel, leftX, leftY + 10f, textPaint)
        canvas.drawText(spec.rightLabel, rightX, rightY + 10f, textPaint)
    }

    private fun drawOrbitalPrism3D(
        canvas: Canvas,
        spec: MotionElementSpec.OrbitalPrism3DSpec,
        palette: ScenePalette,
        frame: Float,
        fps: Int,
        springVal: Float,
        envelope: Float
    ) {
        val timeSec = frame / fps.toFloat()
        val rotY = timeSec * 0.95f * spec.rotationSpeed
        val rotX = 0.45f + sin(timeSec * 0.7f) * 0.22f
        val radius = spec.size * MotionMath.lerp(0.6f, 1.0f, springVal)

        drawGlowOrb(canvas, spec.centerX, spec.centerY, radius * 1.45f, palette.primaryColor, 0.28f * envelope)

        // Orbital elliptical rings
        strokePaint.pathEffect = null
        for (o in 0 until spec.orbitCount) {
            val save = canvas.save()
            canvas.translate(spec.centerX, spec.centerY)
            canvas.rotate(-25f + o * 55f + timeSec * 12f * (if (o % 2 == 0) 1f else -1f))
            val rx = radius * (1.28f + o * 0.22f)
            val ry = radius * (0.42f + o * 0.08f)
            strokePaint.color = withAlpha(if (o % 2 == 0) palette.secondaryColor else palette.accentColor, 0.55f * envelope)
            strokePaint.strokeWidth = 3f
            canvas.drawOval(RectF(-rx, -ry, rx, ry), strokePaint)
            // Orbiting satellite dot
            val satAngle = timeSec * (1.8f + o * 0.6f)
            drawGlowOrb(canvas, cos(satAngle) * rx, sin(satAngle) * ry, 10f, palette.secondaryColor, envelope)
            canvas.restoreToCount(save)
        }

        // True 3D wireframe + translucent faces projection
        val vertices: List<FloatArray>
        val faces: List<IntArray>
        if (spec.shapeKind % 2 == 0) {
            // Octahedron
            vertices = listOf(
                floatArrayOf(0f, -1f, 0f),
                floatArrayOf(1f, 0f, 0f),
                floatArrayOf(0f, 0f, 1f),
                floatArrayOf(-1f, 0f, 0f),
                floatArrayOf(0f, 0f, -1f),
                floatArrayOf(0f, 1f, 0f)
            )
            faces = listOf(
                intArrayOf(0, 1, 2), intArrayOf(0, 2, 3), intArrayOf(0, 3, 4), intArrayOf(0, 4, 1),
                intArrayOf(5, 2, 1), intArrayOf(5, 3, 2), intArrayOf(5, 4, 3), intArrayOf(5, 1, 4)
            )
        } else {
            // Cube
            val d = 0.66f
            vertices = listOf(
                floatArrayOf(-d, -d, -d), floatArrayOf(d, -d, -d),
                floatArrayOf(d, d, -d), floatArrayOf(-d, d, -d),
                floatArrayOf(-d, -d, d), floatArrayOf(d, -d, d),
                floatArrayOf(d, d, d), floatArrayOf(-d, d, d)
            )
            faces = listOf(
                intArrayOf(0, 1, 2, 3), intArrayOf(4, 5, 6, 7),
                intArrayOf(0, 1, 5, 4), intArrayOf(2, 3, 7, 6),
                intArrayOf(1, 2, 6, 5), intArrayOf(0, 3, 7, 4)
            )
        }

        val projected = vertices.map { v ->
            val x1 = v[0] * cos(rotY) - v[2] * sin(rotY)
            val z1 = v[0] * sin(rotY) + v[2] * cos(rotY)
            val y2 = v[1] * cos(rotX) - z1 * sin(rotX)
            val z2 = v[1] * sin(rotX) + z1 * cos(rotX)
            val perspective = 3.2f / (3.2f - z2)
            floatArrayOf(
                spec.centerX + x1 * radius * perspective,
                spec.centerY + y2 * radius * perspective,
                z2
            )
        }

        val sortedFaces = faces.sortedBy { face ->
            face.map { projected[it][2] }.average()
        }

        sortedFaces.forEachIndexed { idx, face ->
            val path = Path().apply {
                val p0 = projected[face[0]]
                moveTo(p0[0], p0[1])
                for (k in 1 until face.size) {
                    val pk = projected[face[k]]
                    lineTo(pk[0], pk[1])
                }
                close()
            }
            val faceColor = if (idx % 2 == 0) palette.primaryColor else palette.secondaryColor
            fillPaint.shader = null
            fillPaint.color = withAlpha(faceColor, 0.16f * envelope)
            canvas.drawPath(path, fillPaint)

            strokePaint.color = withAlpha(palette.primaryColor, 0.85f * envelope)
            strokePaint.strokeWidth = 3f
            canvas.drawPath(path, strokePaint)
        }
    }

    private fun drawGlassFocusCard(
        canvas: Canvas,
        spec: MotionElementSpec.GlassFocusCardSpec,
        palette: ScenePalette,
        springVal: Float,
        envelope: Float,
        transparentBackground: Boolean
    ) {
        // Enforce 10% safe area bounds
        val safeX = spec.x.coerceIn(SAFE_MARGIN_X, STAGE_WIDTH - SAFE_MARGIN_X - spec.width)
        val safeY = spec.y.coerceIn(SAFE_MARGIN_Y, STAGE_HEIGHT - SAFE_MARGIN_Y - spec.height)
        val slideY = (1f - springVal) * 28f
        val rect = RectF(safeX, safeY + slideY, safeX + spec.width, safeY + spec.height + slideY)

        // Subtle diffused elevation shadow
        fillPaint.shader = RadialGradient(
            rect.centerX(), rect.centerY() + 24f, rect.width() * 0.65f,
            withAlpha(palette.inkColor, 0.12f * envelope),
            withAlpha(palette.inkColor, 0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(
            RectF(rect.left - 24f, rect.top - 8f, rect.right + 24f, rect.bottom + 40f),
            40f, 40f, fillPaint
        )
        fillPaint.shader = null

        // Frosted Glass Fill
        fillPaint.color = withAlpha(Color.WHITE, (if (transparentBackground) 0.92f else 0.88f) * envelope)
        canvas.drawRoundRect(rect, 32f, 32f, fillPaint)

        // Subtle inner tint gradient
        fillPaint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            withAlpha(palette.softTintColor, 0.65f * envelope),
            withAlpha(Color.WHITE, 0.25f * envelope),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 32f, 32f, fillPaint)
        fillPaint.shader = null

        // Crisp hairline border
        strokePaint.pathEffect = null
        strokePaint.color = withAlpha(palette.primaryColor, 0.24f * envelope)
        strokePaint.strokeWidth = 2.5f
        canvas.drawRoundRect(rect, 32f, 32f, strokePaint)

        // Accent indicator pill
        val accentBar = RectF(rect.left + 44f, rect.top + 42f, rect.left + 116f, rect.top + 50f)
        fillPaint.color = withAlpha(palette.primaryColor, envelope)
        canvas.drawRoundRect(accentBar, 4f, 4f, fillPaint)

        // Kicker label
        textPaint.typeface = monoTypeface
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = withAlpha(palette.primaryColor, 0.9f * envelope)
        canvas.drawText(spec.kicker, rect.left + 132f, rect.top + 54f, textPaint)

        // Headline (auto-wrapped cleanly inside safe card width)
        textPaint.typeface = primaryTypeface
        textPaint.textSize = 44f
        textPaint.color = withAlpha(palette.inkColor, envelope)
        val innerWidth = (rect.width() - 88f).toInt().coerceAtLeast(100)
        val headlineLayout = StaticLayout.Builder
            .obtain(spec.headline, 0, spec.headline.length, textPaint, innerWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(8f, 1.0f)
            .setMaxLines(2)
            .build()

        canvas.save()
        canvas.translate(rect.left + 44f, rect.top + 88f)
        headlineLayout.draw(canvas)
        canvas.restore()

        // Subline
        textPaint.textSize = 27f
        textPaint.color = withAlpha(palette.inkColor, 0.68f * envelope)
        val subLayout = StaticLayout.Builder
            .obtain(spec.subline, 0, spec.subline.length, textPaint, innerWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(6f, 1.0f)
            .setMaxLines(2)
            .build()
        canvas.save()
        canvas.translate(rect.left + 44f, rect.top + 88f + headlineLayout.height + 24f)
        subLayout.draw(canvas)
        canvas.restore()

        // Bottom metric pill
        if (spec.metricValue.isNotBlank()) {
            textPaint.typeface = monoTypeface
            textPaint.textSize = 23f
            textPaint.color = withAlpha(palette.secondaryColor, 0.95f * envelope)
            canvas.drawText(spec.metricValue, rect.left + 44f, rect.bottom - 36f, textPaint)
        }
    }

    private fun drawLightSweep(
        canvas: Canvas,
        spec: MotionElementSpec.LightSweepSpec,
        palette: ScenePalette,
        frame: Float,
        totalFrames: Int,
        envelope: Float
    ) {
        val progress = MotionMath.interpolate(
            frame = frame,
            inputStart = totalFrames * 0.12f,
            inputEnd = totalFrames * 0.85f,
            outputStart = -0.3f,
            outputEnd = 1.3f,
            easing = MotionMath.easeInOutCubic
        )
        val xPos = progress * STAGE_WIDTH
        val save = canvas.save()
        canvas.rotate(spec.angleDeg, xPos, STAGE_HEIGHT * 0.5f)
        fillPaint.xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        fillPaint.shader = LinearGradient(
            xPos - spec.bandWidth, 0f, xPos + spec.bandWidth, 0f,
            intArrayOf(
                withAlpha(palette.secondaryColor, 0f),
                withAlpha(Color.WHITE, 0.24f * envelope),
                withAlpha(palette.secondaryColor, 0f)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(xPos - spec.bandWidth, -400f, xPos + spec.bandWidth, STAGE_HEIGHT + 400f, fillPaint)
        fillPaint.shader = null
        fillPaint.xfermode = null
        canvas.restoreToCount(save)
    }

    private fun drawIrisRingAccent(
        canvas: Canvas,
        palette: ScenePalette,
        envelope: Float
    ) {
        val maxR = min(STAGE_WIDTH, STAGE_HEIGHT) * 0.6f
        strokePaint.pathEffect = null
        strokePaint.color = withAlpha(palette.secondaryColor, (1f - envelope) * 0.55f)
        strokePaint.strokeWidth = 6f
        canvas.drawCircle(STAGE_WIDTH * 0.5f, STAGE_HEIGHT * 0.5f, maxR * envelope, strokePaint)
    }

    private fun drawGlowOrb(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int,
        alpha: Float
    ) {
        if (radius <= 1f || alpha <= 0.005f) return
        fillPaint.shader = RadialGradient(
            cx, cy, radius,
            withAlpha(color, alpha.coerceIn(0f, 1f)),
            withAlpha(color, 0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, fillPaint)
        fillPaint.shader = null
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun hashFloat(seed: Int): Float {
        var x = seed
        x = ((x ushr 16) xor x) * 0x45d9f3b
        x = ((x ushr 16) xor x) * 0x45d9f3b
        x = (x ushr 16) xor x
        return (x and 0x7FFFFFFF) / 0x7FFFFFFF.toFloat()
    }
}

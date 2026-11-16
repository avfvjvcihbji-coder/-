package com.example.motion

import android.graphics.Color as AndroidColor

enum class VideoResolution(
    val label: String,
    val width: Int,
    val height: Int
) {
    FHD_1080P("1920×1080 (Full HD)", 1920, 1080),
    HD_720P("1280×720 (HD)", 1280, 720),
    PREVIEW_540P("960×540 (Fast)", 960, 540)
}

enum class ExportFormat(val label: String) {
    DUAL_MOV_AND_MP4("خروجی دوگانه (MOV شفاف + MP4 سفید)"),
    TRANSPARENT_MOV_ONLY("فقط MOV شفاف (32-bit RGBA Alpha)"),
    WHITE_MP4_ONLY("فقط MP4 زمینه سفید (#FFFFFF)"),
    PNG_SEQUENCE_ZIP("سکانس PNG شفاف (ZIP) + کد Remotion")
}

enum class PreviewBackground(val label: String) {
    WHITE("سفید (#FFFFFF)"),
    GRAY("خاکستری"),
    CHECKERBOARD("شطرنجی (شفافیت)")
}

data class VideoSettings(
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val fps: Int = 30,
    val defaultDurationSec: Int = 6,
    val animationSpeed: Float = 1.0f,
    val exportFormat: ExportFormat = ExportFormat.DUAL_MOV_AND_MP4
)

data class MotionScenario(
    val coreMessage: String,
    val audienceTakeaway: String,
    val keyVisualFocus: String,
    val visualConcept: String,
    val visualMetaphor: String,
    val objectDesign: String,
    val animationDirection: String,
    val cameraMovement: String,
    val sceneComposition: String,
    val transitionDesign: String,
    val timing: String
)

data class ScenePalette(
    val name: String,
    val primaryColor: Int,
    val secondaryColor: Int,
    val accentColor: Int,
    val inkColor: Int,
    val softTintColor: Int
) {
    companion object {
        val ElectricCyan = ScenePalette(
            name = "Electric Blue & Cyan",
            primaryColor = 0xFF2563EB.toInt(),
            secondaryColor = 0xFF06B6D4.toInt(),
            accentColor = 0xFF6D28D9.toInt(),
            inkColor = 0xFF0F172A.toInt(),
            softTintColor = 0xFFEFF6FF.toInt()
        )
        val DeepPurpleCyan = ScenePalette(
            name = "Deep Purple & Cyan",
            primaryColor = 0xFF6D28D9.toInt(),
            secondaryColor = 0xFF06B6D4.toInt(),
            accentColor = 0xFF2563EB.toInt(),
            inkColor = 0xFF0F172A.toInt(),
            softTintColor = 0xFFF5F3FF.toInt()
        )
        val EmeraldSilver = ScenePalette(
            name = "Emerald & Charcoal",
            primaryColor = 0xFF10B981.toInt(),
            secondaryColor = 0xFF06B6D4.toInt(),
            accentColor = 0xFF2563EB.toInt(),
            inkColor = 0xFF111827.toInt(),
            softTintColor = 0xFFECFDF5.toInt()
        )
        val OrangeCharcoal = ScenePalette(
            name = "Soft Orange & Charcoal",
            primaryColor = 0xFFF97316.toInt(),
            secondaryColor = 0xFF2563EB.toInt(),
            accentColor = 0xFF6D28D9.toInt(),
            inkColor = 0xFF111827.toInt(),
            softTintColor = 0xFFFFF7ED.toInt()
        )
        val CyanCharcoal = ScenePalette(
            name = "Cyan & Silver",
            primaryColor = 0xFF0891B2.toInt(),
            secondaryColor = 0xFF2563EB.toInt(),
            accentColor = 0xFF10B981.toInt(),
            inkColor = 0xFF0F172A.toInt(),
            softTintColor = 0xFFECFEFF.toInt()
        )
    }
}

enum class MotionArchetype(val labelFa: String) {
    NEURAL_NETWORK("شبکه عصبی و پردازش داده"),
    ASCENDING_GROWTH("رشد پلکانی و تکامل مسیر"),
    VELOCITY_STREAM("شتاب خطی و جریان سرعت"),
    DUAL_COMPARISON("تقابل و تعادل دو عنصر"),
    ORBITAL_PRISM("منشور سه‌بعدی و هسته مرکزی"),
    PRECISION_HUD("دقت مهندسی و کالیبراسیون"),
    WAVE_RESONANCE("موج و هم‌افزایی ارتباطی"),
    MODULAR_SYSTEM("معماری ماژولار و ساختار")
}

enum class LayoutMode(val labelFa: String) {
    ASYMMETRIC_LEFT_70("چیدمان ۷۰٪ چپ + ۲۵٪ تنفس بصری"),
    ASYMMETRIC_RIGHT_70("چیدمان ۷۰٪ راست + فضای تنفس چپ"),
    SPLIT_BALANCE("ترکیب‌بندی دوگانه متقارن"),
    CENTER_HERO("کادر سینمایی متمرکز")
}

enum class TransitionType(val labelFa: String) {
    IRIS_REVEAL("بازشدن دیافراگم نوری"),
    KINETIC_SLIDE("جابه‌جایی حرکتی با اینرسی"),
    DEPTH_ZOOM_DISSOLVE("زوم عمقی نرم"),
    LIGHT_SWEEP_WIPE("عبور نور گرادیانی")
}

data class CameraRig(
    val zoomStart: Float = 0.96f,
    val zoomEnd: Float = 1.05f,
    val panXStart: Float = -18f,
    val panXEnd: Float = 18f,
    val panYStart: Float = 10f,
    val panYEnd: Float = -10f,
    val parallaxStrength: Float = 1.0f
)

sealed interface MotionElementSpec {
    val id: String
    val depthLayer: Float // 0.5 (far) .. 1.4 (foreground)
    val enterDelayRatio: Float // 0.0 .. 0.35

    data class NeuralGraphSpec(
        override val id: String,
        override val depthLayer: Float = 1.0f,
        override val enterDelayRatio: Float = 0.05f,
        val centerX: Float,
        val centerY: Float,
        val radiusX: Float,
        val radiusY: Float,
        val nodeCount: Int,
        val seed: Int
    ) : MotionElementSpec

    data class AscendingColumnsSpec(
        override val id: String,
        override val depthLayer: Float = 1.0f,
        override val enterDelayRatio: Float = 0.06f,
        val originX: Float,
        val baselineY: Float,
        val totalWidth: Float,
        val maxHeight: Float,
        val columnCount: Int,
        val seed: Int
    ) : MotionElementSpec

    data class OrbitalPrism3DSpec(
        override val id: String,
        override val depthLayer: Float = 1.15f,
        override val enterDelayRatio: Float = 0.04f,
        val centerX: Float,
        val centerY: Float,
        val size: Float,
        val orbitCount: Int,
        val rotationSpeed: Float,
        val shapeKind: Int // 0 = Octahedron, 1 = Cube, 2 = Hexagonal Prism
    ) : MotionElementSpec

    data class VelocityVectorsSpec(
        override val id: String,
        override val depthLayer: Float = 1.05f,
        override val enterDelayRatio: Float = 0.04f,
        val originX: Float,
        val centerY: Float,
        val spanWidth: Float,
        val streakCount: Int,
        val directionAngleDeg: Float
    ) : MotionElementSpec

    data class DualBalanceSpec(
        override val id: String,
        override val depthLayer: Float = 1.0f,
        override val enterDelayRatio: Float = 0.06f,
        val centerX: Float,
        val centerY: Float,
        val spreadWidth: Float,
        val leftLabel: String,
        val rightLabel: String
    ) : MotionElementSpec

    data class PrecisionHudSpec(
        override val id: String,
        override val depthLayer: Float = 0.95f,
        override val enterDelayRatio: Float = 0.03f,
        val centerX: Float,
        val centerY: Float,
        val outerRadius: Float,
        val ringCount: Int
    ) : MotionElementSpec

    data class BezierFlowSpec(
        override val id: String,
        override val depthLayer: Float = 0.9f,
        override val enterDelayRatio: Float = 0.08f,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val amplitude: Float,
        val waveCount: Int,
        val layerCount: Int
    ) : MotionElementSpec

    data class GlassFocusCardSpec(
        override val id: String,
        override val depthLayer: Float = 1.2f,
        override val enterDelayRatio: Float = 0.12f,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val kicker: String,
        val headline: String,
        val subline: String,
        val metricValue: String
    ) : MotionElementSpec

    data class FloatingParticlesSpec(
        override val id: String,
        override val depthLayer: Float = 0.7f,
        override val enterDelayRatio: Float = 0.0f,
        val count: Int,
        val seed: Int
    ) : MotionElementSpec

    data class LightSweepSpec(
        override val id: String,
        override val depthLayer: Float = 1.3f,
        override val enterDelayRatio: Float = 0.1f,
        val angleDeg: Float,
        val bandWidth: Float
    ) : MotionElementSpec
}

data class MotionScene(
    val id: String,
    val paragraphIndex: Int,
    val title: String,
    val sourceText: String,
    val durationSeconds: Int,
    val fps: Int,
    val archetype: MotionArchetype,
    val layoutMode: LayoutMode,
    val palette: ScenePalette,
    val scenario: MotionScenario,
    val cameraRig: CameraRig,
    val transitionIn: TransitionType,
    val transitionOut: TransitionType,
    val elements: List<MotionElementSpec>,
    val variationSeed: Int,
    val isAiSampleMode: Boolean = true
) {
    val totalFrames: Int
        get() = (durationSeconds * fps).coerceAtLeast(30)
}

data class MotionProject(
    val id: String,
    val scriptText: String,
    val scenes: List<MotionScene>,
    val settings: VideoSettings,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val totalFrames: Int
        get() = scenes.sumOf { it.totalFrames }.coerceAtLeast(30)

    val totalDurationSeconds: Int
        get() = scenes.sumOf { it.durationSeconds }
}

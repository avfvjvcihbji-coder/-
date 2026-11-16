package com.example.ai

import com.example.motion.CameraRig
import com.example.motion.LayoutMode
import com.example.motion.MotionArchetype
import com.example.motion.MotionElementSpec
import com.example.motion.MotionFrameRenderer.Companion.SAFE_MARGIN_X
import com.example.motion.MotionFrameRenderer.Companion.SAFE_MARGIN_Y
import com.example.motion.MotionFrameRenderer.Companion.STAGE_HEIGHT
import com.example.motion.MotionFrameRenderer.Companion.STAGE_WIDTH
import com.example.motion.MotionScenario
import com.example.motion.MotionScene
import com.example.motion.ScenePalette
import com.example.motion.TransitionType
import com.example.motion.VideoSettings
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.abs

/**
 * Sample / offline concept-driven AI repository used when Firebase AI Logic is not enabled.
 * Ensures every paragraph receives an independent, non-repeating visual metaphor, palette,
 * camera rig, transition, and procedural composition.
 */
class SampleMotionAiRepository : MotionAiRepository {

    override val providerName: String = "Sample Concept Engine (Firebase Required for Gemini)"
    override val isSampleMode: Boolean = true

    override suspend fun analyzeScriptToScenes(
        scriptText: String,
        settings: VideoSettings
    ): Result<List<MotionScene>> {
        val paragraphs = scriptText
            .split(Regex("\\n\\s*\\n|\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (paragraphs.isEmpty()) {
            return Result.failure(IllegalArgumentException("لطفاً حداقل یک جمله یا پاراگراف برای تحلیل وارد کنید."))
        }

        // Simulate realistic analysis step without blocking UI
        delay(380)

        var previousArchetype: MotionArchetype? = null
        var previousLayout: LayoutMode? = null
        var previousPaletteName: String? = null

        val scenes = paragraphs.mapIndexed { index, paragraph ->
            val scene = buildDistinctScene(
                paragraphIndex = index,
                paragraph = paragraph,
                settings = settings,
                variationSeed = index * 13 + abs(paragraph.hashCode() % 97),
                avoidArchetype = previousArchetype,
                avoidLayout = previousLayout,
                avoidPaletteName = previousPaletteName
            )
            previousArchetype = scene.archetype
            previousLayout = scene.layoutMode
            previousPaletteName = scene.palette.name
            scene
        }
        return Result.success(scenes)
    }

    override suspend fun regenerateScene(
        scene: MotionScene,
        settings: VideoSettings,
        variationSeed: Int
    ): Result<MotionScene> {
        delay(260)
        val regenerated = buildDistinctScene(
            paragraphIndex = scene.paragraphIndex,
            paragraph = scene.sourceText,
            settings = settings,
            variationSeed = variationSeed,
            avoidArchetype = scene.archetype,
            avoidLayout = scene.layoutMode,
            avoidPaletteName = scene.palette.name
        ).copy(
            id = scene.id,
            durationSeconds = scene.durationSeconds
        )
        return Result.success(regenerated)
    }

    private fun buildDistinctScene(
        paragraphIndex: Int,
        paragraph: String,
        settings: VideoSettings,
        variationSeed: Int,
        avoidArchetype: MotionArchetype?,
        avoidLayout: LayoutMode?,
        avoidPaletteName: String?
    ): MotionScene {
        val archetype = pickArchetype(paragraph, variationSeed, avoidArchetype)
        val palette = pickPalette(archetype, variationSeed, avoidPaletteName)
        val layoutMode = pickLayout(archetype, variationSeed, avoidLayout)
        val transitions = pickTransitions(archetype, variationSeed)
        val cameraRig = pickCameraRig(archetype, variationSeed)
        val scenario = buildScenario(paragraph, archetype, layoutMode, cameraRig, transitions.first)
        val headline = extractHeadline(paragraph)
        val subline = extractSubline(paragraph, headline)
        val elements = composeElements(
            archetype = archetype,
            layoutMode = layoutMode,
            headline = headline,
            subline = subline,
            paragraphIndex = paragraphIndex,
            seed = variationSeed
        )

        return MotionScene(
            id = UUID.randomUUID().toString(),
            paragraphIndex = paragraphIndex,
            title = "صحنه ${paragraphIndex + 1}: ${archetype.labelFa}",
            sourceText = paragraph,
            durationSeconds = settings.defaultDurationSec,
            fps = settings.fps,
            archetype = archetype,
            layoutMode = layoutMode,
            palette = palette,
            scenario = scenario,
            cameraRig = cameraRig,
            transitionIn = transitions.first,
            transitionOut = transitions.second,
            elements = elements,
            variationSeed = variationSeed,
            isAiSampleMode = true
        )
    }

    private fun pickArchetype(
        text: String,
        seed: Int,
        avoid: MotionArchetype?
    ): MotionArchetype {
        val lower = text.lowercase()
        val matched = when {
            containsAny(lower, "هوش", "ai", "شبکه", "عصبی", "داده", "الگوریتم", "پردازش", "مدل", "neural") ->
                MotionArchetype.NEURAL_NETWORK
            containsAny(lower, "رشد", "پیشرفت", "آینده", "صعود", "افزایش", "توسعه", "موفقیت", "growth", "scale") ->
                MotionArchetype.ASCENDING_GROWTH
            containsAny(lower, "سرعت", "شتاب", "سریع", "لحظه", "تأخیر", "زمان", "فوری", "speed", "fast") ->
                MotionArchetype.VELOCITY_STREAM
            containsAny(lower, "مقایسه", "تفاوت", "تضاد", "رقابت", "در برابر", "تعادل", "دو", "vs", "compare") ->
                MotionArchetype.DUAL_COMPARISON
            containsAny(lower, "دقت", "مهندسی", "کنترل", "امنیت", "اسکن", "پایش", "precision", "security") ->
                MotionArchetype.PRECISION_HUD
            containsAny(lower, "موج", "صدا", "ارتباط", "انرژی", "جریان", "حس", "آهنگ", "wave", "flow") ->
                MotionArchetype.WAVE_RESONANCE
            containsAny(lower, "ساختار", "معماری", "سیستم", "پلتفرم", "ماژول", "بخش", "system", "modular") ->
                MotionArchetype.MODULAR_SYSTEM
            else -> {
                val all = MotionArchetype.entries
                all[abs(seed) % all.size]
            }
        }
        if (matched == avoid) {
            val alternatives = MotionArchetype.entries.filter { it != avoid }
            return alternatives[abs(seed + 3) % alternatives.size]
        }
        return matched
    }

    private fun pickPalette(
        archetype: MotionArchetype,
        seed: Int,
        avoidName: String?
    ): ScenePalette {
        val candidates = listOf(
            ScenePalette.ElectricCyan,
            ScenePalette.DeepPurpleCyan,
            ScenePalette.EmeraldSilver,
            ScenePalette.OrangeCharcoal,
            ScenePalette.CyanCharcoal
        )
        val preferred = when (archetype) {
            MotionArchetype.NEURAL_NETWORK -> ScenePalette.DeepPurpleCyan
            MotionArchetype.ASCENDING_GROWTH -> ScenePalette.EmeraldSilver
            MotionArchetype.VELOCITY_STREAM -> ScenePalette.OrangeCharcoal
            MotionArchetype.DUAL_COMPARISON -> ScenePalette.ElectricCyan
            MotionArchetype.PRECISION_HUD -> ScenePalette.CyanCharcoal
            else -> candidates[abs(seed) % candidates.size]
        }
        if (preferred.name == avoidName) {
            val filtered = candidates.filter { it.name != avoidName }
            return filtered[abs(seed + 1) % filtered.size]
        }
        return preferred
    }

    private fun pickLayout(
        archetype: MotionArchetype,
        seed: Int,
        avoid: LayoutMode?
    ): LayoutMode {
        val preferred = when (archetype) {
            MotionArchetype.DUAL_COMPARISON -> LayoutMode.SPLIT_BALANCE
            MotionArchetype.ORBITAL_PRISM -> LayoutMode.CENTER_HERO
            else -> if (seed % 2 == 0) LayoutMode.ASYMMETRIC_LEFT_70 else LayoutMode.ASYMMETRIC_RIGHT_70
        }
        if (preferred == avoid) {
            val filtered = LayoutMode.entries.filter { it != avoid }
            return filtered[abs(seed + 2) % filtered.size]
        }
        return preferred
    }

    private fun pickTransitions(archetype: MotionArchetype, seed: Int): Pair<TransitionType, TransitionType> {
        val all = TransitionType.entries
        val tIn = when (archetype) {
            MotionArchetype.PRECISION_HUD -> TransitionType.IRIS_REVEAL
            MotionArchetype.VELOCITY_STREAM -> TransitionType.KINETIC_SLIDE
            MotionArchetype.NEURAL_NETWORK -> TransitionType.DEPTH_ZOOM_DISSOLVE
            else -> all[abs(seed) % all.size]
        }
        val tOut = all[(all.indexOf(tIn) + 1) % all.size]
        return tIn to tOut
    }

    private fun pickCameraRig(archetype: MotionArchetype, seed: Int): CameraRig {
        val dir = if (seed % 2 == 0) 1f else -1f
        return when (archetype) {
            MotionArchetype.VELOCITY_STREAM -> CameraRig(
                zoomStart = 0.95f, zoomEnd = 1.07f,
                panXStart = -36f * dir, panXEnd = 36f * dir,
                panYStart = 8f, panYEnd = -8f,
                parallaxStrength = 1.25f
            )
            MotionArchetype.ASCENDING_GROWTH -> CameraRig(
                zoomStart = 0.97f, zoomEnd = 1.04f,
                panXStart = -14f * dir, panXEnd = 14f * dir,
                panYStart = 28f, panYEnd = -24f,
                parallaxStrength = 1.05f
            )
            MotionArchetype.ORBITAL_PRISM, MotionArchetype.NEURAL_NETWORK -> CameraRig(
                zoomStart = 0.93f, zoomEnd = 1.06f,
                panXStart = 18f * dir, panXEnd = -18f * dir,
                panYStart = -10f, panYEnd = 12f,
                parallaxStrength = 1.15f
            )
            else -> CameraRig(
                zoomStart = 0.98f, zoomEnd = 1.04f,
                panXStart = -20f * dir, panXEnd = 20f * dir,
                panYStart = 12f * dir, panYEnd = -12f * dir,
                parallaxStrength = 1.0f
            )
        }
    }

    private fun composeElements(
        archetype: MotionArchetype,
        layoutMode: LayoutMode,
        headline: String,
        subline: String,
        paragraphIndex: Int,
        seed: Int
    ): List<MotionElementSpec> {
        val elements = mutableListOf<MotionElementSpec>()

        // Ambient depth particles (subtle, never noisy)
        elements += MotionElementSpec.FloatingParticlesSpec(
            id = "particles_$paragraphIndex",
            count = 14 + (seed % 6),
            seed = seed + 7
        )

        // Hero graphic region computed inside Safe Area respecting 70-75% composition + breathing room
        val cardOnLeft = layoutMode == LayoutMode.ASYMMETRIC_LEFT_70
        val visualCenterX = when (layoutMode) {
            LayoutMode.ASYMMETRIC_LEFT_70 -> STAGE_WIDTH * 0.62f
            LayoutMode.ASYMMETRIC_RIGHT_70 -> STAGE_WIDTH * 0.36f
            LayoutMode.SPLIT_BALANCE -> STAGE_WIDTH * 0.50f
            LayoutMode.CENTER_HERO -> STAGE_WIDTH * 0.50f
        }
        val visualCenterY = when (layoutMode) {
            LayoutMode.CENTER_HERO -> STAGE_HEIGHT * 0.42f
            else -> STAGE_HEIGHT * 0.50f
        }

        when (archetype) {
            MotionArchetype.NEURAL_NETWORK -> {
                elements += MotionElementSpec.NeuralGraphSpec(
                    id = "neural_$paragraphIndex",
                    centerX = visualCenterX,
                    centerY = visualCenterY,
                    radiusX = 340f,
                    radiusY = 260f,
                    nodeCount = 7 + (seed % 3),
                    seed = seed
                )
                elements += MotionElementSpec.PrecisionHudSpec(
                    id = "neural_hud_$paragraphIndex",
                    centerX = visualCenterX,
                    centerY = visualCenterY,
                    outerRadius = 310f,
                    ringCount = 2
                )
            }
            MotionArchetype.ASCENDING_GROWTH -> {
                elements += MotionElementSpec.AscendingColumnsSpec(
                    id = "growth_$paragraphIndex",
                    originX = visualCenterX - 280f,
                    baselineY = STAGE_HEIGHT * 0.72f,
                    totalWidth = 560f,
                    maxHeight = 420f,
                    columnCount = 5 + (seed % 2),
                    seed = seed
                )
                elements += MotionElementSpec.BezierFlowSpec(
                    id = "growth_flow_$paragraphIndex",
                    startX = SAFE_MARGIN_X,
                    startY = STAGE_HEIGHT * 0.78f,
                    endX = STAGE_WIDTH - SAFE_MARGIN_X,
                    endY = STAGE_HEIGHT * 0.24f,
                    amplitude = 45f,
                    waveCount = 2,
                    layerCount = 2
                )
            }
            MotionArchetype.VELOCITY_STREAM -> {
                elements += MotionElementSpec.VelocityVectorsSpec(
                    id = "velocity_$paragraphIndex",
                    originX = SAFE_MARGIN_X + 40f,
                    centerY = visualCenterY,
                    spanWidth = STAGE_WIDTH - SAFE_MARGIN_X * 2.2f,
                    streakCount = 9,
                    directionAngleDeg = if (seed % 2 == 0) -6f else 6f
                )
                elements += MotionElementSpec.OrbitalPrism3DSpec(
                    id = "velocity_prism_$paragraphIndex",
                    centerX = visualCenterX + 120f,
                    centerY = visualCenterY,
                    size = 135f,
                    orbitCount = 2,
                    rotationSpeed = 1.6f,
                    shapeKind = 0
                )
            }
            MotionArchetype.DUAL_COMPARISON -> {
                elements += MotionElementSpec.DualBalanceSpec(
                    id = "dual_$paragraphIndex",
                    centerX = STAGE_WIDTH * 0.50f,
                    centerY = STAGE_HEIGHT * 0.44f,
                    spreadWidth = 680f,
                    leftLabel = "ساختار A",
                    rightLabel = "الگوی B"
                )
            }
            MotionArchetype.ORBITAL_PRISM, MotionArchetype.MODULAR_SYSTEM -> {
                elements += MotionElementSpec.OrbitalPrism3DSpec(
                    id = "prism_$paragraphIndex",
                    centerX = visualCenterX,
                    centerY = visualCenterY,
                    size = 185f,
                    orbitCount = 3,
                    rotationSpeed = 1.1f,
                    shapeKind = if (archetype == MotionArchetype.MODULAR_SYSTEM) 1 else 0
                )
                elements += MotionElementSpec.BezierFlowSpec(
                    id = "prism_flow_$paragraphIndex",
                    startX = SAFE_MARGIN_X,
                    startY = visualCenterY + 120f,
                    endX = STAGE_WIDTH - SAFE_MARGIN_X,
                    endY = visualCenterY - 120f,
                    amplitude = 55f,
                    waveCount = 2,
                    layerCount = 2
                )
            }
            MotionArchetype.PRECISION_HUD -> {
                elements += MotionElementSpec.PrecisionHudSpec(
                    id = "hud_$paragraphIndex",
                    centerX = visualCenterX,
                    centerY = visualCenterY,
                    outerRadius = 265f,
                    ringCount = 3
                )
                elements += MotionElementSpec.OrbitalPrism3DSpec(
                    id = "hud_core_$paragraphIndex",
                    centerX = visualCenterX,
                    centerY = visualCenterY,
                    size = 105f,
                    orbitCount = 1,
                    rotationSpeed = 0.85f,
                    shapeKind = 0
                )
            }
            MotionArchetype.WAVE_RESONANCE -> {
                elements += MotionElementSpec.BezierFlowSpec(
                    id = "wave_$paragraphIndex",
                    startX = SAFE_MARGIN_X,
                    startY = visualCenterY,
                    endX = STAGE_WIDTH - SAFE_MARGIN_X,
                    endY = visualCenterY,
                    amplitude = 140f,
                    waveCount = 3,
                    layerCount = 3
                )
            }
        }

        // Glass Focus Card positioned cleanly within 10% Safe Area without clipping hero geometry
        val cardWidth = 640f
        val cardHeight = 320f
        val cardX = when (layoutMode) {
            LayoutMode.ASYMMETRIC_LEFT_70 -> SAFE_MARGIN_X
            LayoutMode.ASYMMETRIC_RIGHT_70 -> STAGE_WIDTH - SAFE_MARGIN_X - cardWidth
            LayoutMode.SPLIT_BALANCE -> (STAGE_WIDTH - cardWidth) * 0.5f
            LayoutMode.CENTER_HERO -> (STAGE_WIDTH - cardWidth) * 0.5f
        }
        val cardY = when (layoutMode) {
            LayoutMode.SPLIT_BALANCE, LayoutMode.CENTER_HERO -> STAGE_HEIGHT - SAFE_MARGIN_Y - cardHeight
            else -> if (cardOnLeft) STAGE_HEIGHT * 0.28f else STAGE_HEIGHT * 0.34f
        }

        elements += MotionElementSpec.GlassFocusCardSpec(
            id = "card_$paragraphIndex",
            x = cardX,
            y = cardY,
            width = cardWidth,
            height = cardHeight,
            kicker = "SCENE 0${paragraphIndex + 1} · ${archetype.name}",
            headline = headline,
            subline = subline,
            metricValue = "1920×1080 · 30FPS · SAMPLE"
        )

        elements += MotionElementSpec.LightSweepSpec(
            id = "sweep_$paragraphIndex",
            angleDeg = 22f + (seed % 14),
            bandWidth = 220f
        )

        return elements
    }

    private fun buildScenario(
        paragraph: String,
        archetype: MotionArchetype,
        layoutMode: LayoutMode,
        cameraRig: CameraRig,
        transitionIn: TransitionType
    ): MotionScenario {
        val snippet = paragraph.take(64).let { if (paragraph.length > 64) "$it…" else it }
        return MotionScenario(
            coreMessage = "انتقال مفهوم محوری: «$snippet»",
            audienceTakeaway = "درک بصری سریع ارتباط میان ایده متن و استعاره حرکتی اختصاصی صحنه",
            keyVisualFocus = archetype.labelFa,
            visualConcept = "طراحی صحنه مینیمال در سبک Apple Event و Linear با تمرکز بر عمق شفاف و کنتراست تمیز روی زمینه سفید",
            visualMetaphor = when (archetype) {
                MotionArchetype.NEURAL_NETWORK -> "شبکه سیناپسی پویا با جریان بسته‌های نورانی داده میان گره‌های مرکزی"
                MotionArchetype.ASCENDING_GROWTH -> "ستون‌های شیشه‌ای پلکانی همراه با منحنی صعودی تکامل"
                MotionArchetype.VELOCITY_STREAM -> "خطوط آیرودینامیک شتاب و منشور پیشرو با دنباله حرکتی نرم"
                MotionArchetype.DUAL_COMPARISON -> "تقابل دو هندسه متمایز متصل با پل تنش و تکیه‌گاه تعادل"
                MotionArchetype.ORBITAL_PRISM -> "هسته منشوری سه‌بعدی چرخان در مدارهای هم‌مرکز"
                MotionArchetype.PRECISION_HUD -> "دایره‌های هم‌مرکز کالیبراسیون و اسکنر دقیق مهندسی"
                MotionArchetype.WAVE_RESONANCE -> "امواج بزیه چندلایه با تپش هماهنگ و هم‌افزا"
                MotionArchetype.MODULAR_SYSTEM -> "مکعب شیشه‌ای سه‌بعدی با مدارهای ساختاریافته"
            },
            objectDesign = "آبجکت‌های وکتوری و سه‌بعدی پروجکت‌شده با حاشیه ظریف، گرادیان کنترل‌شده و کارت شیشه‌ای خوانا داخل Safe Area",
            animationDirection = "ورود فنری (Spring damping=14) با تاخیر پلکانی لایه‌ها، حرکت میانی شناور و خروج کنترل‌شده بدون قطع ناگهانی",
            cameraMovement = "زوم نرم از ${"%.2f".format(cameraRig.zoomStart)} به ${"%.2f".format(cameraRig.zoomEnd)} همراه با حرکت افقی پارالاکس",
            sceneComposition = "${layoutMode.labelFa} با رعایت حاشیه امن ۱۰ درصدی در کادر ۱۹۲۰×۱۰۸۰",
            transitionDesign = transitionIn.labelFa,
            timing = "ورود: 0.0s–0.7s | اوج و مکث پویا: 0.7s–85% | خروج نرم: 15% پایانی"
        )
    }

    private fun extractHeadline(paragraph: String): String {
        val cleaned = paragraph.replace(Regex("\\s+"), " ").trim()
        val firstSentence = cleaned.split(Regex("[.!؟?،,]")).firstOrNull()?.trim().orEmpty()
        val candidate = if (firstSentence.length >= 8) firstSentence else cleaned
        return if (candidate.length > 56) candidate.take(53).trimEnd() + "…" else candidate
    }

    private fun extractSubline(paragraph: String, headline: String): String {
        val cleaned = paragraph.replace(Regex("\\s+"), " ").trim()
        val remainder = cleaned.removePrefix(headline.removeSuffix("…")).trimStart(' ', '.', '،', ',', '!', '؟', '?')
        val sub = if (remainder.length >= 6) remainder else "تجسم مفهومی اختصاصی با انیمیشن فریم‌محور"
        return if (sub.length > 84) sub.take(81).trimEnd() + "…" else sub
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any { text.contains(it) }
}

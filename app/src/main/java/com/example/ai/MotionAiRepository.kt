package com.example.ai

import com.example.motion.MotionScene
import com.example.motion.VideoSettings

enum class AiProviderKind(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val requiresFirebase: Boolean
) {
    FIREBASE_GEMINI(
        id = "firebase_gemini",
        displayName = "Firebase AI Logic (Gemini 3.8 Flash)",
        subtitle = "نیازمند فعال‌سازی Firebase App Check و AI Logic",
        requiresFirebase = true
    ),
    SAMPLE_CONCEPT_ENGINE(
        id = "sample_concept",
        displayName = "موتور نمونه تحلیل مفهوم (آفلاین)",
        subtitle = "تولید سناریوهای نمونه متمایز بدون اتصال به سرور ابری",
        requiresFirebase = false
    ),
    FUTURE_EXTERNAL_PROVIDER(
        id = "external_custom",
        displayName = "ارائه‌دهنده سفارشی (معماری توسعه‌پذیر)",
        subtitle = "قابل اتصال به مدل‌های آینده (OpenAI / Claude / Local)",
        requiresFirebase = false
    )
}

data class CustomProviderConfig(
    val selectedProviderId: String = AiProviderKind.SAMPLE_CONCEPT_ENGINE.id,
    val customEndpointUrl: String = "",
    val customModelName: String = "gemini-3.8-flash",
    val hasStoredCustomApiKey: Boolean = false
)

interface MotionAiRepository {
    val providerName: String
    val isSampleMode: Boolean

    suspend fun analyzeScriptToScenes(
        scriptText: String,
        settings: VideoSettings
    ): Result<List<MotionScene>>

    suspend fun regenerateScene(
        scene: MotionScene,
        settings: VideoSettings,
        variationSeed: Int
    ): Result<MotionScene>
}

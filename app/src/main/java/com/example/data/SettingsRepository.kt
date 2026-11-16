package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ai.AiProviderKind
import com.example.ai.CustomProviderConfig
import com.example.motion.ExportFormat
import com.example.motion.VideoResolution
import com.example.motion.VideoSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "motion_studio_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val RESOLUTION = stringPreferencesKey("video_resolution")
        val FPS = intPreferencesKey("video_fps")
        val DEFAULT_DURATION = intPreferencesKey("default_duration_sec")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")
        val PROVIDER_ID = stringPreferencesKey("ai_provider_id")
        val CUSTOM_ENDPOINT = stringPreferencesKey("custom_endpoint_url")
        val CUSTOM_MODEL = stringPreferencesKey("custom_model_name")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_provider_api_key")
    }

    val videoSettingsFlow: Flow<VideoSettings> = context.studioDataStore.data.map { prefs ->
        val res = runCatching {
            VideoResolution.valueOf(prefs[Keys.RESOLUTION] ?: VideoResolution.FHD_1080P.name)
        }.getOrDefault(VideoResolution.FHD_1080P)

        val fmt = runCatching {
            ExportFormat.valueOf(prefs[Keys.EXPORT_FORMAT] ?: ExportFormat.DUAL_MOV_AND_MP4.name)
        }.getOrDefault(ExportFormat.DUAL_MOV_AND_MP4)

        VideoSettings(
            resolution = res,
            fps = (prefs[Keys.FPS] ?: 30).coerceIn(24, 60),
            defaultDurationSec = (prefs[Keys.DEFAULT_DURATION] ?: 6).coerceIn(3, 20),
            animationSpeed = (prefs[Keys.ANIMATION_SPEED] ?: 1.0f).coerceIn(0.5f, 2.0f),
            exportFormat = fmt
        )
    }

    val providerConfigFlow: Flow<CustomProviderConfig> = context.studioDataStore.data.map { prefs ->
        CustomProviderConfig(
            selectedProviderId = prefs[Keys.PROVIDER_ID] ?: AiProviderKind.SAMPLE_CONCEPT_ENGINE.id,
            customEndpointUrl = prefs[Keys.CUSTOM_ENDPOINT] ?: "",
            customModelName = prefs[Keys.CUSTOM_MODEL] ?: "gemini-3.8-flash",
            hasStoredCustomApiKey = !prefs[Keys.CUSTOM_API_KEY].isNullOrBlank()
        )
    }

    suspend fun updateVideoSettings(settings: VideoSettings) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.RESOLUTION] = settings.resolution.name
            prefs[Keys.FPS] = settings.fps
            prefs[Keys.DEFAULT_DURATION] = settings.defaultDurationSec
            prefs[Keys.ANIMATION_SPEED] = settings.animationSpeed
            prefs[Keys.EXPORT_FORMAT] = settings.exportFormat.name
        }
    }

    suspend fun updateProviderSelection(providerId: String) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.PROVIDER_ID] = providerId
        }
    }

    suspend fun saveCustomProviderCredentials(
        endpointUrl: String,
        modelName: String,
        apiKey: String?
    ) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_ENDPOINT] = endpointUrl.trim()
            prefs[Keys.CUSTOM_MODEL] = modelName.trim().ifEmpty { "gemini-3.8-flash" }
            if (!apiKey.isNullOrBlank()) {
                prefs[Keys.CUSTOM_API_KEY] = apiKey.trim()
            }
        }
    }

    suspend fun clearCustomApiKey() {
        context.studioDataStore.edit { prefs ->
            prefs.remove(Keys.CUSTOM_API_KEY)
        }
    }
}

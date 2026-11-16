package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.CustomProviderConfig
import com.example.ai.MotionAiRepository
import com.example.ai.SampleMotionAiRepository
import com.example.data.SettingsRepository
import com.example.export.DualExportManager
import com.example.export.ExportArtifact
import com.example.export.ExportUiState
import com.example.export.RemotionCodeGenerator
import com.example.motion.ExportFormat
import com.example.motion.MotionFrameRenderer
import com.example.motion.MotionProject
import com.example.motion.MotionScene
import com.example.motion.PreviewBackground
import com.example.motion.VideoResolution
import com.example.motion.VideoSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface GenerationUiState {
    data object Idle : GenerationUiState
    data object Analyzing : GenerationUiState
    data class Ready(val project: MotionProject) : GenerationUiState
    data class Error(val message: String) : GenerationUiState
}

class MotionStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val aiRepository: MotionAiRepository = SampleMotionAiRepository()

    val frameRenderer: MotionFrameRenderer by lazy {
        val vazir = ResourcesCompat.getFont(application, R.font.vazirmatn)
            ?: android.graphics.Typeface.SANS_SERIF
        val mono = ResourcesCompat.getFont(application, R.font.jetbrains_mono)
            ?: android.graphics.Typeface.MONOSPACE
        MotionFrameRenderer(primaryTypeface = vazir, monoTypeface = mono)
    }

    private val exportManager by lazy {
        DualExportManager(application, frameRenderer)
    }

    val videoSettings: StateFlow<VideoSettings> = settingsRepository.videoSettingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, VideoSettings())

    val providerConfig: StateFlow<CustomProviderConfig> = settingsRepository.providerConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, CustomProviderConfig())

    private val _scriptText = MutableStateFlow(
        "هوش مصنوعی داده‌های پیچیده را به بینش‌های شفاف و تصمیم‌های دقیق تبدیل می‌کند.\n\n" +
            "رشد مداوم سیستم‌های هوشمند، سرعت اجرا و کیفیت خروجی را چند برابر کرده است."
    )
    val scriptText: StateFlow<String> = _scriptText.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    private val _selectedSceneIndex = MutableStateFlow<Int?>(null) // null = full unified video
    val selectedSceneIndex: StateFlow<Int?> = _selectedSceneIndex.asStateFlow()

    private val _currentFrame = MutableStateFlow(0)
    val currentFrame: StateFlow<Int> = _currentFrame.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _previewBackground = MutableStateFlow(PreviewBackground.WHITE)
    val previewBackground: StateFlow<PreviewBackground> = _previewBackground.asStateFlow()

    private val _previewResolution = MutableStateFlow(VideoResolution.HD_720P)
    val previewResolution: StateFlow<VideoResolution> = _previewResolution.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var playbackJob: Job? = null
    private var exportJob: Job? = null
    private var regenerationCounter = 1

    val isSampleAiMode: Boolean
        get() = aiRepository.isSampleMode

    fun updateScriptText(value: String) {
        _scriptText.value = value
    }

    fun applySamplePrompt(sample: String) {
        _scriptText.value = sample
    }

    fun updateDurationSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(3, 20)
        viewModelScope.launch {
            settingsRepository.updateVideoSettings(
                videoSettings.value.copy(defaultDurationSec = clamped)
            )
            // Update active project scenes duration cleanly if already generated
            val currentReady = _generationState.value as? GenerationUiState.Ready ?: return@launch
            val updatedScenes = currentReady.project.scenes.map { it.copy(durationSeconds = clamped) }
            val updatedProject = currentReady.project.copy(
                scenes = updatedScenes,
                settings = currentReady.project.settings.copy(defaultDurationSec = clamped)
            )
            _generationState.value = GenerationUiState.Ready(updatedProject)
            _currentFrame.value = 0
        }
    }

    fun updateSceneDuration(sceneIndex: Int, seconds: Int) {
        val currentReady = _generationState.value as? GenerationUiState.Ready ?: return
        val clamped = seconds.coerceIn(3, 20)
        val updatedScenes = currentReady.project.scenes.mapIndexed { idx, scene ->
            if (idx == sceneIndex) scene.copy(durationSeconds = clamped) else scene
        }
        _generationState.value = GenerationUiState.Ready(
            currentReady.project.copy(scenes = updatedScenes)
        )
        _currentFrame.value = 0
    }

    fun generateMotion() {
        val text = _scriptText.value.trim()
        if (text.isEmpty()) {
            _generationState.value = GenerationUiState.Error("لطفاً ابتدا متن یا فیلمنامه خود را وارد کنید.")
            return
        }
        pausePlayback()
        _exportState.value = ExportUiState.Idle
        viewModelScope.launch {
            _generationState.value = GenerationUiState.Analyzing
            val result = aiRepository.analyzeScriptToScenes(text, videoSettings.value)
            result.fold(
                onSuccess = { scenes ->
                    val project = MotionProject(
                        id = UUID.randomUUID().toString(),
                        scriptText = text,
                        scenes = scenes,
                        settings = videoSettings.value
                    )
                    _selectedSceneIndex.value = null
                    _currentFrame.value = 0
                    _generationState.value = GenerationUiState.Ready(project)
                    startPlayback()
                },
                onFailure = { error ->
                    _generationState.value = GenerationUiState.Error(
                        error.localizedMessage ?: "خطا در تحلیل فیلمنامه."
                    )
                }
            )
        }
    }

    fun regenerateScene(sceneIndex: Int) {
        val currentReady = _generationState.value as? GenerationUiState.Ready ?: return
        val target = currentReady.project.scenes.getOrNull(sceneIndex) ?: return
        pausePlayback()
        regenerationCounter += 7
        viewModelScope.launch {
            val result = aiRepository.regenerateScene(target, videoSettings.value, regenerationCounter)
            result.fold(
                onSuccess = { newScene ->
                    val updatedScenes = currentReady.project.scenes.toMutableList().apply {
                        set(sceneIndex, newScene)
                    }
                    _generationState.value = GenerationUiState.Ready(
                        currentReady.project.copy(scenes = updatedScenes)
                    )
                    _currentFrame.value = 0
                    _exportState.value = ExportUiState.Idle
                    startPlayback()
                },
                onFailure = { err ->
                    _toastMessage.value = err.localizedMessage ?: "خطا در بازطراحی صحنه"
                }
            )
        }
    }

    fun selectSceneForPreview(index: Int?) {
        _selectedSceneIndex.value = index
        _currentFrame.value = 0
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pausePlayback() else startPlayback()
    }

    fun startPlayback() {
        val ready = _generationState.value as? GenerationUiState.Ready ?: return
        val totalFrames = activeTotalFrames(ready.project)
        if (totalFrames <= 0) return

        playbackJob?.cancel()
        _isPlaying.value = true
        playbackJob = viewModelScope.launch {
            val fps = ready.project.settings.fps.coerceAtLeast(1)
            val delayMs = (1000L / fps).coerceAtLeast(12L)
            while (isActive && _isPlaying.value) {
                delay(delayMs)
                val next = _currentFrame.value + 1
                _currentFrame.value = if (next >= totalFrames) 0 else next
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun restartPlayback() {
        _currentFrame.value = 0
        startPlayback()
    }

    fun stepFrame(delta: Int) {
        pausePlayback()
        val ready = _generationState.value as? GenerationUiState.Ready ?: return
        val totalFrames = activeTotalFrames(ready.project)
        if (totalFrames <= 0) return
        val next = (_currentFrame.value + delta).coerceIn(0, totalFrames - 1)
        _currentFrame.value = next
    }

    fun seekToFrame(frame: Int) {
        val ready = _generationState.value as? GenerationUiState.Ready ?: return
        val totalFrames = activeTotalFrames(ready.project)
        _currentFrame.value = frame.coerceIn(0, (totalFrames - 1).coerceAtLeast(0))
    }

    fun setPreviewBackground(bg: PreviewBackground) {
        _previewBackground.value = bg
    }

    fun setPreviewResolution(res: VideoResolution) {
        _previewResolution.value = res
    }

    fun activeTotalFrames(project: MotionProject): Int {
        val idx = _selectedSceneIndex.value
        return if (idx != null) {
            project.scenes.getOrNull(idx)?.totalFrames ?: project.totalFrames
        } else {
            project.totalFrames
        }
    }

    fun startExport(formatOverride: ExportFormat? = null) {
        val ready = _generationState.value as? GenerationUiState.Ready ?: return
        if (_exportState.value is ExportUiState.Rendering) return
        pausePlayback()

        val selectedScene = _selectedSceneIndex.value?.let { ready.project.scenes.getOrNull(it) }
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _exportState.value = ExportUiState.Rendering(
                stepTitle = "آماده‌سازی Composition و فریم‌ها...",
                currentFrame = 0,
                totalFrames = activeTotalFrames(ready.project),
                overallProgress = 0.02f
            )
            val result = exportManager.exportProject(
                project = ready.project,
                singleScene = selectedScene,
                formatOverride = formatOverride,
                onProgress = { progress ->
                    _exportState.value = progress
                }
            )
            result.fold(
                onSuccess = { readyExport ->
                    _exportState.value = readyExport
                },
                onFailure = { error ->
                    _exportState.value = ExportUiState.Error(
                        error.localizedMessage ?: "خطا در رندر ویدیو."
                    )
                }
            )
        }
    }

    fun saveArtifactToDownloads(artifact: ExportArtifact) {
        viewModelScope.launch {
            exportManager.saveArtifactToDownloads(artifact).fold(
                onSuccess = { msg -> _toastMessage.value = msg },
                onFailure = { err -> _toastMessage.value = err.localizedMessage ?: "خطا در ذخیره فایل" }
            )
        }
    }

    fun buildShareIntent(artifact: ExportArtifact): Intent =
        exportManager.createShareIntent(artifact)

    fun getRemotionTsxPreview(): String {
        val ready = _generationState.value as? GenerationUiState.Ready ?: return ""
        return RemotionCodeGenerator.generateTsxForProject(ready.project)
    }

    fun updateVideoSettings(newSettings: VideoSettings) {
        viewModelScope.launch {
            settingsRepository.updateVideoSettings(newSettings)
            val currentReady = _generationState.value as? GenerationUiState.Ready ?: return@launch
            val updatedScenes = currentReady.project.scenes.map { it.copy(fps = newSettings.fps) }
            _generationState.value = GenerationUiState.Ready(
                currentReady.project.copy(scenes = updatedScenes, settings = newSettings)
            )
        }
    }

    fun selectProvider(providerId: String) {
        viewModelScope.launch {
            settingsRepository.updateProviderSelection(providerId)
            _toastMessage.value = "پروفایل موتور هوش مصنوعی به‌روزرسانی شد."
        }
    }

    fun saveCustomProvider(endpoint: String, model: String, apiKey: String?) {
        viewModelScope.launch {
            settingsRepository.saveCustomProviderCredentials(endpoint, model, apiKey)
            _toastMessage.value = "تنظیمات ارائه‌دهنده با امنیت محلی ذخیره شد."
        }
    }

    fun clearCustomApiKey() {
        viewModelScope.launch {
            settingsRepository.clearCustomApiKey()
            _toastMessage.value = "کلید API سفارشی حذف شد."
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        exportJob?.cancel()
    }
}

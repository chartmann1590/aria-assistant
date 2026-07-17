package com.aria.assistant.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.assistant.domain.model.VoiceConfig
import com.aria.assistant.domain.repository.SettingsRepository
import com.aria.assistant.engine.AriaLogger
import com.aria.assistant.engine.LlmEngine
import com.aria.assistant.engine.ModelDownloadManager
import com.aria.assistant.translation.TranslationStatus
import com.aria.assistant.translation.UiLanguage
import com.aria.assistant.translation.UiTranslationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingDownloadState(
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val isDownloading: Boolean = false,
    val isInitializing: Boolean = false,
    val isReady: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val downloadManager: ModelDownloadManager,
    private val llmEngine: LlmEngine,
    private val translationManager: UiTranslationManager
) : ViewModel() {

    private val _downloadState = MutableStateFlow(OnboardingDownloadState())
    val downloadState: StateFlow<OnboardingDownloadState> = _downloadState.asStateFlow()

    private val _voiceConfig = MutableStateFlow(VoiceConfig())
    val voiceConfig: StateFlow<VoiceConfig> = _voiceConfig.asStateFlow()
    val supportedUiLanguages: List<UiLanguage> = translationManager.supportedLanguages
    val translationStatus: StateFlow<TranslationStatus> = translationManager.status

    companion object {
        private const val MODEL_E2B_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
        private const val MODEL_E2B_FILENAME = "gemma-4-E2B-it.litertlm"
        private const val MODEL_E4B_URL = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
        private const val MODEL_E4B_FILENAME = "gemma-4-E4B-it.litertlm"
    }

    private fun modelUrlFor(model: String): Pair<String, String> {
        return if (model == "E4B") MODEL_E4B_URL to MODEL_E4B_FILENAME
        else MODEL_E2B_URL to MODEL_E2B_FILENAME
    }

    init {
        viewModelScope.launch {
            settingsRepository.getVoiceConfig().collect { _voiceConfig.value = it }
        }
        viewModelScope.launch {
            downloadManager.state.collect { state ->
                when (state) {
                    is ModelDownloadManager.DownloadState.Idle -> {}
                    is ModelDownloadManager.DownloadState.Downloading -> {
                        _downloadState.value = _downloadState.value.copy(
                            isDownloading = true,
                            progress = state.progress,
                            bytesDownloaded = state.bytesLoaded,
                            totalBytes = state.totalBytes
                        )
                    }
                    is ModelDownloadManager.DownloadState.Complete -> {
                        _downloadState.value = _downloadState.value.copy(
                            isDownloading = false,
                            isInitializing = true,
                            progress = 1f
                        )
                        AriaLogger.d("OnboardingVM", "Model downloaded, initializing engine...")
                        llmEngine.initialize(state.modelPath)
                    }
                    is ModelDownloadManager.DownloadState.Error -> {
                        _downloadState.value = _downloadState.value.copy(
                            isDownloading = false,
                            error = state.message
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            llmEngine.isReady.collect { ready ->
                if (ready) {
                    _downloadState.value = _downloadState.value.copy(
                        isInitializing = false,
                        isReady = true
                    )
                }
            }
        }
    }

    fun startModelDownload() {
        if (_downloadState.value.isDownloading || _downloadState.value.isReady) return
        val (url, filename) = modelUrlFor("E2B")
        val cached = downloadManager.getCachedModelPath(filename)
        if (cached != null) {
            AriaLogger.d("OnboardingVM", "Model already cached, initializing")
            _downloadState.value = _downloadState.value.copy(isInitializing = true, progress = 1f)
            viewModelScope.launch { llmEngine.initialize(cached) }
        } else {
            AriaLogger.d("OnboardingVM", "Starting model download")
            _downloadState.value = _downloadState.value.copy(isDownloading = true)
            viewModelScope.launch { downloadManager.downloadModel(url, filename) }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete(true)
        }
    }

    fun updateUiLanguage(languageTag: String) {
        if (_voiceConfig.value.uiLanguage == languageTag) return
        viewModelScope.launch {
            try {
                translationManager.prepareLanguage(languageTag)
                settingsRepository.updateVoiceConfig(
                    _voiceConfig.value.copy(uiLanguage = languageTag)
                )
            } catch (_: Exception) {
                // The download error is exposed through translationStatus for the UI.
            }
        }
    }

    fun requestBatteryOptimizationExemption(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            )
        }
    }
}

package com.aria.assistant.presentation.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.assistant.ads.AdManager
import com.aria.assistant.billing.BillingManager
import com.aria.assistant.domain.model.VoiceConfig
import com.aria.assistant.domain.repository.SettingsRepository
import com.aria.assistant.engine.AriaTTS
import com.aria.assistant.engine.VoiceInfo
import com.aria.assistant.engine.VoiceModelManager
import com.aria.assistant.engine.VoiceDownloadState
import com.aria.assistant.translation.TranslationStatus
import com.aria.assistant.translation.UiLanguage
import com.aria.assistant.translation.UiTranslationManager
import com.aria.assistant.web.WebVerificationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val voiceModelManager: VoiceModelManager,
    private val ariaTTS: AriaTTS,
    private val billingManager: BillingManager,
    private val translationManager: UiTranslationManager,
    private val adManager: AdManager
) : ViewModel() {

    val appVersion: String = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "?"
    } catch (_: Exception) { "?" }

    private val _voiceConfig = MutableStateFlow(VoiceConfig())
    val voiceConfig: StateFlow<VoiceConfig> = _voiceConfig.asStateFlow()

    val isPremium: StateFlow<Boolean> = billingManager.isPremium
    val privacyOptionsRequired: StateFlow<Boolean> = adManager.privacyOptionsRequired

    val availableVoices: List<VoiceInfo> = voiceModelManager.availableVoices

    val downloadStates = voiceModelManager.downloadStates

    val supportedUiLanguages: List<UiLanguage> = translationManager.supportedLanguages
    val translationStatus: StateFlow<TranslationStatus> = translationManager.status

    init {
        viewModelScope.launch {
            settingsRepository.getVoiceConfig().collect { config ->
                _voiceConfig.value = config
                ariaTTS.updateSpeed(config.ttsSpeed)
                ariaTTS.updatePitch(config.ttsPitch)
            }
        }
    }

    fun showAdPrivacyOptions(activity: Activity) {
        adManager.showPrivacyOptions(activity)
    }

    fun updateWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateVoiceConfig(_voiceConfig.value.copy(wakeWordEnabled = enabled))
        }
    }

    fun updateWakeWordSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            settingsRepository.updateVoiceConfig(_voiceConfig.value.copy(wakeWordSensitivity = sensitivity))
        }
    }

    fun updateTtsSpeed(speed: Float) {
        ariaTTS.updateSpeed(speed)
        viewModelScope.launch {
            settingsRepository.updateVoiceConfig(_voiceConfig.value.copy(ttsSpeed = speed))
        }
    }

    fun updateTtsPitch(pitch: Float) {
        ariaTTS.updatePitch(pitch)
        viewModelScope.launch {
            settingsRepository.updateVoiceConfig(_voiceConfig.value.copy(ttsPitch = pitch))
        }
    }

    fun updateTemperatureUnit(unit: String) {
        viewModelScope.launch {
            settingsRepository.updateVoiceConfig(_voiceConfig.value.copy(temperatureUnit = unit))
        }
    }

    fun updateUiLanguage(languageTag: String) {
        if (_voiceConfig.value.uiLanguage == languageTag) return
        viewModelScope.launch {
            try {
                translationManager.prepareLanguage(languageTag)
                settingsRepository.updateVoiceConfig(_voiceConfig.value.copy(uiLanguage = languageTag))
            } catch (_: Exception) {
                // The manager exposes the actionable download error through translationStatus.
            }
        }
    }

    fun updateWebVerificationMode(mode: WebVerificationMode) {
        if (_voiceConfig.value.webVerificationMode == mode) return
        viewModelScope.launch {
            settingsRepository.updateVoiceConfig(_voiceConfig.value.copy(webVerificationMode = mode))
        }
    }

    fun updatePrivacyMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePrivacyMode(enabled)
        }
    }

    fun updateSelectedModel(model: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedModel(model)
        }
    }

    fun updateSelectedVoice(voiceId: String) {
        if (_voiceConfig.value.selectedVoice == voiceId) return
        viewModelScope.launch {
            ariaTTS.switchVoice(voiceId)
            settingsRepository.updateVoiceConfig(_voiceConfig.value.copy(selectedVoice = voiceId))
        }
    }

    fun downloadVoice(voiceId: String) {
        viewModelScope.launch {
            voiceModelManager.downloadVoice(voiceId)
        }
    }

    fun isVoiceDownloaded(voiceId: String): Boolean {
        return voiceModelManager.isVoiceDownloaded(voiceId)
    }

    fun previewVoice(voiceId: String) {
        viewModelScope.launch {
            if (!voiceModelManager.isVoiceDownloaded(voiceId)) return@launch
            val currentVoice = _voiceConfig.value.selectedVoice
            ariaTTS.switchVoice(voiceId)
            ariaTTS.speak("Hello, I'm Aria. How can I help you today?")
            if (currentVoice != voiceId) {
                ariaTTS.switchVoice(currentVoice)
            }
        }
    }
}

package com.aria.assistant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.assistant.billing.BillingManager
import com.aria.assistant.domain.model.VoiceConfig
import com.aria.assistant.domain.repository.SettingsRepository
import com.aria.assistant.engine.AriaTTS
import com.aria.assistant.engine.VoiceInfo
import com.aria.assistant.engine.VoiceModelManager
import com.aria.assistant.engine.VoiceDownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val voiceModelManager: VoiceModelManager,
    private val ariaTTS: AriaTTS,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _voiceConfig = MutableStateFlow(VoiceConfig())
    val voiceConfig: StateFlow<VoiceConfig> = _voiceConfig.asStateFlow()

    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    val availableVoices: List<VoiceInfo> = voiceModelManager.availableVoices

    val downloadStates = voiceModelManager.downloadStates

    init {
        viewModelScope.launch {
            settingsRepository.getVoiceConfig().collect { config ->
                _voiceConfig.value = config
                ariaTTS.updateSpeed(config.ttsSpeed)
                ariaTTS.updatePitch(config.ttsPitch)
            }
        }
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

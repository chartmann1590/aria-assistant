package com.aria.assistant.domain.model

import com.aria.assistant.web.WebVerificationMode

data class VoiceConfig(
    val wakeWordEnabled: Boolean = true,
    val wakeWordSensitivity: Float = 0.5f,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val language: String = "en-US",
    val uiLanguage: String = "en",
    val selectedVoice: String = "en_US-amy-medium",
    val temperatureUnit: String = "celsius",
    /**
     * When true (the default), transcription uses on-device Whisper whenever
     * the model is present. Audio only leaves the device when the user
     * explicitly opts out AND the Whisper model is missing.
     */
    val preferOnDeviceStt: Boolean = true,
    val selectedModel: String = "E2B",
    val webVerificationMode: WebVerificationMode = WebVerificationMode.ALWAYS_FACTUAL
)

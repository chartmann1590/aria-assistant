package com.aria.assistant.domain.model

data class VoiceConfig(
    val wakeWordEnabled: Boolean = true,
    val wakeWordSensitivity: Float = 0.5f,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val language: String = "en-US",
    val selectedVoice: String = "en_US-amy-medium",
    val temperatureUnit: String = "celsius"
)

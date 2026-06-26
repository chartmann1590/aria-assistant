package com.aria.assistant.engine

import android.content.Context
import com.aria.assistant.billing.FeatureGate
import com.aria.assistant.billing.Feature
import com.aria.assistant.domain.model.VoiceConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class TtsEngineType {
    ANDROID, PIPER
}

@Singleton
class AriaTTS @Inject constructor(
    @ApplicationContext private val context: Context,
    private val piperEngine: PiperTtsEngine,
    private val fallback: AndroidTTSFallback,
    private val featureGate: FeatureGate
) {
    @Volatile var isSpeaking = false
        private set

    private var activeEngine: TtsEngineType = TtsEngineType.ANDROID
    private var currentVoiceId: String = ""
    private var currentSpeed: Float = 1.0f
    private var currentPitch: Float = 1.0f

    fun initialize(baseModelDir: String) {
        java.io.File(baseModelDir).mkdirs()
    }

    fun voiceLoaded(voiceId: String): Boolean {
        return piperEngine.isLoaded(voiceId)
    }

    fun switchVoice(voiceId: String) {
        if (voiceId == currentVoiceId && voiceLoaded(voiceId)) return
        if (voiceId.isBlank()) return

        currentVoiceId = voiceId

        val voiceInfo = VoiceModelManager.voiceInfoForId(voiceId)
        val voiceForFeature = voiceInfo?.let { Feature.PREMIUM_VOICES }

        if (voiceForFeature != null && voiceInfo!!.isPremium && !featureGate.isAllowed(Feature.PREMIUM_VOICES)) {
            AriaLogger.d("AriaTTS", "Premium voice blocked: $voiceId (not subscribed)")
            activeEngine = TtsEngineType.ANDROID
            return
        }

        if (piperEngine.load(voiceId)) {
            activeEngine = TtsEngineType.PIPER
            AriaLogger.d("AriaTTS", "Switched to Piper voice: $voiceId")
        } else {
            activeEngine = TtsEngineType.ANDROID
            AriaLogger.d("AriaTTS", "Piper unavailable, fallback to Android TTS")
        }
    }

    fun updateSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.5f, 2.0f)
    }

    fun updatePitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
    }

    suspend fun speak(text: String) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext
        isSpeaking = true
        try {
            when (activeEngine) {
                TtsEngineType.PIPER -> {
                    piperEngine.speak(text, speed = currentSpeed)
                }
                TtsEngineType.ANDROID -> {
                    fallback.speak(text, speed = currentSpeed, pitch = currentPitch)
                }
            }
        } catch (e: Exception) {
            AriaLogger.e("AriaTTS", "Speak failed: ${e.message}")
        } finally {
            isSpeaking = false
        }
    }

    fun stopSpeaking() {
        piperEngine.stop()
        fallback.stop()
        isSpeaking = false
    }

    fun release() {
        stopSpeaking()
        piperEngine.release()
        fallback.release()
    }
}

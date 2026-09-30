package com.aria.assistant.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.DetectionMode
import com.rementia.openwakeword.lib.model.WakeWordModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wake word detection with two tiers:
 *
 * 1. Neural: OpenWakeWord ONNX model running on the mic stream (real "phrase"
 *    detection, immune to claps/door slams). Models ship in APK assets
 *    (assets/wakeword/). The community "Hey Jarvis" model is currently mapped
 *    as the Aria wake phrase until a dedicated "Hey Aria" model is trained —
 *    openWakeWord can train custom models from synthetic TTS samples.
 * 2. Fallback: raw-energy gate (previous behavior), used only when the neural
 *    engine cannot initialize, so wake word support degrades instead of dying.
 */
@Singleton
class WakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var neuralEngine: WakeWordEngine? = null
    private var neuralJob: Job? = null
    private var neuralScope: CoroutineScope? = null

    // --- Energy fallback state ---
    private var audioRecord: AudioRecord? = null
    private var fallbackScope: CoroutineScope? = null
    private var fallbackJob: Job? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: Flow<Boolean> = _isListening.asStateFlow()

    private val _detected = MutableStateFlow(false)
    val detected: Flow<Boolean> = _detected.asStateFlow()

    private val _usingNeural = MutableStateFlow(false)
    val usingNeural: Flow<Boolean> = _usingNeural.asStateFlow()

    private val sampleRate = 16000
    private val chunkSize = 1280
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(chunkSize * 2)

    private var hasMicPermission = false

    fun start(threshold: Float = 0.5f, onDetected: () -> Unit) {
        if (_isListening.value) return
        hasMicPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMicPermission) return

        _detected.value = false
        if (startNeural(threshold, onDetected)) return
        startEnergyFallback(onDetected)
    }

    /** Maps the user-facing sensitivity slider (0..1, higher = more sensitive)
     *  to an OWW detection threshold (lower = more sensitive). */
    private fun neuralThreshold(sensitivity: Float): Float =
        (0.75f - sensitivity.coerceIn(0f, 1f) * 0.45f)

    private fun startNeural(sensitivity: Float, onDetected: () -> Unit): Boolean {
        // ONNX Runtime's JNI lib resolves OrtGetApiBase at dlopen time and does
        // not declare a DT_NEEDED on the core lib — the core MUST be loaded
        // first. OrtEnvironment's static init does exactly that; several
        // consumers (sherpa's bundled runtime) may not have run yet.
        runCatching { ai.onnxruntime.OrtEnvironment.getEnvironment() }
            .onFailure { AriaLogger.e("WakeWordDetector", "ONNX Runtime unavailable: ${it.message}") }

        val engine = neuralEngine ?: runCatching {
            WakeWordEngine(
                context = context,
                models = listOf(
                    WakeWordModel(
                        name = WAKE_PHRASE,
                        modelPath = WAKE_MODEL_ASSET,
                        threshold = neuralThreshold(sensitivity)
                    )
                ),
                detectionMode = DetectionMode.SINGLE_BEST,
                detectionCooldownMs = 2000L
            )
        }.getOrElse { failure ->
            AriaLogger.e("WakeWordDetector", "Neural engine unavailable (${failure.message}); using energy fallback")
            return false
        }
        neuralEngine = engine

        // Re-create the engine if the sensitivity changed enough to matter —
        // thresholds are baked into WakeWordModel. Cheap: sessions are small.
        val wanted = neuralThreshold(sensitivity)
        val current = activeNeuralThreshold
        if (current != null && kotlin.math.abs(current - wanted) > 0.01f) {
            runCatching { engine.release() }
            neuralEngine = null
            return startNeural(sensitivity, onDetected)
        }
        activeNeuralThreshold = wanted

        return try {
            neuralScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            neuralJob = neuralScope?.launch {
                engine.detections.collect {
                    AriaLogger.d("WakeWordDetector", "NEURAL WAKE (${it.model.name}, score=${it.score})")
                    _detected.value = true
                    onDetected()
                }
            }
            engine.start()
            _usingNeural.value = true
            _isListening.value = true
            AriaLogger.d("WakeWordDetector", "Listening (neural, threshold=$wanted)")
            true
        } catch (e: Exception) {
            AriaLogger.e("WakeWordDetector", "Neural start failed (${e.message}); using energy fallback")
            neuralJob?.cancel(); neuralJob = null
            neuralScope?.cancel(); neuralScope = null
            false
        }
    }

    private var activeNeuralThreshold: Float? = null

    private fun startEnergyFallback(onDetected: () -> Unit) {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            AriaLogger.e("WakeWordDetector", "AudioRecord init failed")
            cleanup()
            return
        }

        fallbackScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        audioRecord!!.startRecording()
        _usingNeural.value = false
        _isListening.value = true
        AriaLogger.d("WakeWordDetector", "Listening (energy mode)")

        fallbackJob = fallbackScope?.launch {
            val buffer = ShortArray(chunkSize)
            var streak = 0
            val requiredStreak = 12

            while (isActive) {
                val read = audioRecord!!.read(buffer, 0, chunkSize)
                if (read <= 0) continue

                val triggered = energyDetected(buffer, read)
                if (triggered) {
                    streak++
                    if (streak >= requiredStreak) {
                        AriaLogger.d("WakeWordDetector", "WAKE WORD TRIGGERED (streak=$streak)")
                        _detected.value = true
                        onDetected()
                        streak = 0
                    }
                } else {
                    if (streak > 0) streak--
                }
            }
        }
    }

    private fun energyDetected(buffer: ShortArray, length: Int): Boolean {
        var sum = 0.0
        var peak = 0.0
        for (i in 0 until length) {
            val abs = Math.abs(buffer[i].toDouble() / 32768.0)
            sum += abs * abs
            if (abs > peak) peak = abs
        }
        val rms = Math.sqrt(sum / length)
        return rms > 0.025 && peak > 0.12
    }

    fun stop() {
        neuralJob?.cancel()
        neuralJob = null
        neuralScope?.cancel()
        neuralScope = null
        neuralEngine?.stop()

        fallbackJob?.cancel()
        fallbackJob = null
        fallbackScope?.cancel()
        fallbackScope = null
        cleanup()

        _isListening.value = false
        _detected.value = false
    }

    private fun cleanup() {
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
    }

    companion object {
        const val WAKE_PHRASE = "Hey Aria"
        const val WAKE_MODEL_ASSET = "wakeword/hey_aria.onnx"
    }
}

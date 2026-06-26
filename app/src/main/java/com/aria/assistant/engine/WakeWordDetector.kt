package com.aria.assistant.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
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

@Singleton
class WakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var audioRecord: AudioRecord? = null
    private var scope: CoroutineScope? = null
    private var recordingJob: Job? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: Flow<Boolean> = _isListening.asStateFlow()

    private val _detected = MutableStateFlow(false)
    val detected: Flow<Boolean> = _detected.asStateFlow()

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

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        audioRecord!!.startRecording()
        _isListening.value = true
        AriaLogger.d("WakeWordDetector", "Listening (energy mode)")

        recordingJob = scope?.launch {
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
        recordingJob?.cancel()
        recordingJob = null
        scope?.cancel()
        scope = null
        cleanup()
        _isListening.value = false
        _detected.value = false
    }

    private fun cleanup() {
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
    }
}

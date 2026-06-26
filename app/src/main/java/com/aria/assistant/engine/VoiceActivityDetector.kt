package com.aria.assistant.engine

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceActivityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var energyThreshold = 0.02f
    private val silenceThresholdMs = 1500L
    private val maxRecordingMs = 15000L
    private val sampleRate = 16000

    fun setSensitivity(threshold: Float) {
        energyThreshold = threshold.coerceIn(0.005f, 0.1f)
    }

    fun collectSpeechSegment(audioChunks: Flow<ShortArray>): Flow<ShortArray> = flow {
        val buffer = mutableListOf<Short>()
        var silenceStart = -1L
        val startTime = System.currentTimeMillis()
        var speechDetected = false

        val collected = audioChunks.first { chunk ->
            buffer.addAll(chunk.toList())

            val rms = computeRms(chunk)
            val isSpeech = rms >= energyThreshold

            if (isSpeech) {
                speechDetected = true
                silenceStart = -1
            } else if (speechDetected && silenceStart < 0) {
                silenceStart = System.currentTimeMillis()
            }

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > maxRecordingMs) return@first true
            if (speechDetected && silenceStart > 0 &&
                System.currentTimeMillis() - silenceStart > silenceThresholdMs
            ) return@first true

            false
        }

        emit(buffer.toShortArray())
    }

    private fun computeRms(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (s in samples) {
            val normalized = s / 32768.0
            sum += normalized * normalized
        }
        return sqrt(sum / samples.size).toFloat()
    }
}

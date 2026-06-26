package com.aria.assistant.engine

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperSTT @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recognizer: OfflineRecognizer? = null

    fun initialize(modelDir: String) {
        if (recognizer != null) return
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(
                sampleRate = 16000,
                featureDim = 80
            ),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = "$modelDir/encoder.onnx",
                    decoder = "$modelDir/decoder.onnx",
                    language = "en",
                    task = "transcribe"
                ),
                numThreads = 2,
                debug = false
            ),
            decodingMethod = "greedy_search"
        )
        recognizer = OfflineRecognizer(context.assets, config)
    }

    suspend fun transcribe(audioData: ShortArray): String = withContext(Dispatchers.IO) {
        val rec = recognizer ?: return@withContext ""
        val stream = rec.createStream()
        try {
            stream.acceptWaveform(audioData.toFloatArray(), 16000)
            rec.decode(stream)
            rec.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    fun release() {
        recognizer?.release()
        recognizer = null
    }
}

private fun ShortArray.toFloatArray(): FloatArray {
    val floatArray = FloatArray(size)
    for (i in indices) {
        floatArray[i] = this[i] / 32768f
    }
    return floatArray
}

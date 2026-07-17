package com.aria.assistant.engine

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperSTT @Inject constructor(
) {
    private var recognizer: OfflineRecognizer? = null

    fun initialize(modelDir: String): Boolean {
        if (recognizer != null) return true
        val encoder = File(modelDir, ENCODER_FILENAME)
        val decoder = File(modelDir, DECODER_FILENAME)
        val tokens = File(modelDir, TOKENS_FILENAME)
        if (!hasCompleteModel(modelDir)) {
            AriaLogger.e("WhisperSTT", "Whisper model files are missing or incomplete; using Android STT")
            return false
        }
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(
                sampleRate = 16000,
                featureDim = 80
            ),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = encoder.absolutePath,
                    decoder = decoder.absolutePath,
                    language = "en",
                    task = "transcribe"
                ),
                tokens = tokens.absolutePath,
                numThreads = 2,
                debug = false
            ),
            decodingMethod = "greedy_search"
        )
        // These models are downloaded into app-private storage, so sherpa must use
        // its file-backed constructor. Supplying AssetManager makes native code
        // interpret absolute paths as APK asset names and abort the process.
        recognizer = OfflineRecognizer(config = config)
        return true
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

    companion object {
        const val ENCODER_FILENAME = "encoder.onnx"
        const val DECODER_FILENAME = "decoder.onnx"
        const val TOKENS_FILENAME = "tokens.txt"

        private const val MIN_ENCODER_BYTES = 1_000_000L
        private const val MIN_DECODER_BYTES = 10_000_000L
        private const val MIN_TOKENS_BYTES = 100_000L

        fun hasCompleteModel(modelDir: String): Boolean =
            File(modelDir, ENCODER_FILENAME).length() >= MIN_ENCODER_BYTES &&
                File(modelDir, DECODER_FILENAME).length() >= MIN_DECODER_BYTES &&
                File(modelDir, TOKENS_FILENAME).length() >= MIN_TOKENS_BYTES
    }
}

private fun ShortArray.toFloatArray(): FloatArray {
    val floatArray = FloatArray(size)
    for (i in indices) {
        floatArray[i] = this[i] / 32768f
    }
    return floatArray
}

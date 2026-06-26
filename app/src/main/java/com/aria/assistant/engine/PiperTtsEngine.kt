package com.aria.assistant.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.LongBuffer
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PiperTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var session: OrtSession? = null
    private var env: OrtEnvironment? = null
    private var currentVoiceId: String? = null
    private var currentSpeed: Float = 1.0f
    private var audioTrack: AudioTrack? = null
    private var stopRequested = false
    private var sampleRate: Int = 22050
    private var phonemeIdMap: Map<String, List<Int>> = emptyMap()
    private var bosToken: Int = 1
    private var eosToken: Int = 2
    private var noiseScale: Float = 0.667f
    private var lengthScale: Float = 1.0f
    private var noiseW: Float = 0.8f
    private val g2p = EnglishG2P()

    fun isLoaded(voiceId: String): Boolean {
        return currentVoiceId == voiceId && session != null
    }

    fun load(voiceId: String): Boolean {
        if (isLoaded(voiceId)) return true
        release()

        val modelFile = File(context.filesDir, "models/piper/$voiceId.onnx")
        val configFile = File(context.filesDir, "models/piper/$voiceId.onnx.json")
        val dictDir = File(context.filesDir, "models/piper/cmudict.dict")

        if (!modelFile.exists()) return false

        currentVoiceId = voiceId

        try {
            if (configFile.exists()) {
                parseConfig(configFile)
            }

            if (dictDir.exists()) {
                g2p.load(dictDir)
            }

            env = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.setIntraOpNumThreads(2)
            session = env!!.createSession(modelFile.absolutePath, sessionOptions)

            initAudioTrack()
            AriaLogger.d("PiperTtsEngine", "Loaded voice via ONNX: $voiceId")
            return true
        } catch (e: Exception) {
            AriaLogger.e("PiperTtsEngine", "Failed to load: ${e.message}")
            release()
            return false
        }
    }

    private fun parseConfig(configFile: File) {
        try {
            val json = JSONObject(configFile.readText())
            val audio = json.optJSONObject("audio")
            if (audio != null) {
                sampleRate = audio.optInt("sample_rate", 22050)
            }

            val inference = json.optJSONObject("inference")
            if (inference != null) {
                noiseScale = inference.optDouble("noise_scale", 0.667).toFloat()
                lengthScale = inference.optDouble("length_scale", 1.0).toFloat()
                noiseW = inference.optDouble("noise_w", 0.8).toFloat()

                val phonemeIdMapJson = inference.optJSONObject("phoneme_id_map")
                if (phonemeIdMapJson != null) {
                    val map = mutableMapOf<String, List<Int>>()
                    for (key in phonemeIdMapJson.keys()) {
                        val ids = phonemeIdMapJson.get(key)
                        if (ids is org.json.JSONArray) {
                            map[key] = (0 until ids.length()).map { ids.getInt(it) }
                        }
                    }
                    phonemeIdMap = map
                }
            }

            val numSymbols = json.optInt("num_symbols", 0)
            if (numSymbols > 0) {
                val emptyId = phonemeIdMap.values.flatten().maxOrNull()?.plus(1) ?: 0
                val maxId = phonemeIdMap.values.flatten().maxOrNull() ?: 0
                bosToken = (maxId + 1).coerceAtMost(numSymbols - 1)
                eosToken = (maxId + 2).coerceAtMost(numSymbols - 1)
            }
        } catch (e: Exception) {
            AriaLogger.e("PiperTtsEngine", "Config parse error: ${e.message}")
        }
    }

    private fun initAudioTrack() {
        val bufLength = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate)

        val attr = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .build()

        audioTrack = AudioTrack(
            attr, format, bufLength, AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    suspend fun speak(text: String, speed: Float = 1.0f) = withContext(Dispatchers.IO) {
        val sess = session ?: return@withContext
        val track = audioTrack ?: return@withContext
        if (!g2p.isLoaded()) return@withContext

        stopRequested = false
        currentSpeed = speed.coerceIn(0.5f, 2.0f)

        try {
            val phonemeIds = g2p.sentenceToPhonemeIds(text, phonemeIdMap, bosToken, eosToken)
            if (phonemeIds.isEmpty()) return@withContext

            val seqLen = phonemeIds.size.toLong()
            val inputShape = longArrayOf(1, seqLen)
            val inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(phonemeIds.map { it.toLong() }.toLongArray()), inputShape)
            val inputLengths = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(seqLen)), longArrayOf(1))

            val inputMap = LinkedHashMap<String, OnnxTensor>()
            inputMap["input"] = inputTensor
            inputMap["input_lengths"] = inputLengths

            if (hasInputNamed(sess, "scales")) {
                val scaleTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(floatArrayOf(
                    noiseScale, lengthScale / currentSpeed, noiseW
                )), longArrayOf(3))
                inputMap["scales"] = scaleTensor
            }

            val result = sess.run(inputMap)
            val outputTensor = result.get("output") as? OnnxTensor ?: return@withContext
            val audioData = outputTensor.floatBuffer

            val audioFloats = FloatArray(audioData.remaining())
            audioData.get(audioFloats)

            track.play()
            val audioShorts = ShortArray(audioFloats.size)
            for (i in audioFloats.indices) {
                val sample = (audioFloats[i] * 32767f).coerceIn(-32768f, 32767f)
                audioShorts[i] = sample.toInt().toShort()
            }

            val chunkSize = sampleRate / 5
            var offset = 0
            while (offset < audioShorts.size && !stopRequested) {
                val chunk = audioShorts.sliceArray(offset until (offset + chunkSize).coerceAtMost(audioShorts.size))
                track.write(chunk, 0, chunk.size)
                offset += chunkSize
            }

            try {
                inputTensor.close()
                inputLengths.close()
                if (inputMap.containsKey("scales")) inputMap["scales"]?.close()
                result.close()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            AriaLogger.e("PiperTtsEngine", "ONNX inference failed: ${e.message}")
        } finally {
            try { track.pause(); track.flush() } catch (_: Exception) {}
        }
    }

    private fun hasInputNamed(session: OrtSession, name: String): Boolean {
        return try {
            session.inputInfo.entries.any { it.key.equals(name, ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        stopRequested = true
        try { audioTrack?.stop(); audioTrack?.flush() } catch (_: Exception) {}
    }

    fun release() {
        stop()
        try { audioTrack?.release() } catch (_: Exception) {}
        audioTrack = null
        try { session?.close() } catch (_: Exception) {}
        session = null
        env = null
        currentVoiceId = null
        phonemeIdMap = emptyMap()
    }
}

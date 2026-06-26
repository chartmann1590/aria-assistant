package com.aria.assistant.engine

import android.content.Context
import com.aria.assistant.engine.AriaLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class VoiceInfo(
    val id: String,
    val displayName: String,
    val accent: String,
    val isPremium: Boolean,
    val isDefault: Boolean = false
)

sealed class VoiceDownloadState {
    object Idle : VoiceDownloadState()
    data class Downloading(val progress: Float) : VoiceDownloadState()
    object Complete : VoiceDownloadState()
    data class Error(val message: String) : VoiceDownloadState()
}

@Singleton
class VoiceModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _downloadStates = MutableStateFlow<Map<String, VoiceDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, VoiceDownloadState>> = _downloadStates.asStateFlow()

    val availableVoices: List<VoiceInfo>
        get() = ALL_VOICES

    fun getVoiceDir(): String {
        return File(context.filesDir, "models/piper").absolutePath.also {
            File(it).mkdirs()
        }
    }

    fun isVoiceDownloaded(voiceId: String): Boolean {
        return File(context.filesDir, "models/piper/$voiceId.onnx").exists()
    }

    fun ensureVoiceDownloaded(voiceId: String): Boolean {
        return isVoiceDownloaded(voiceId)
    }

    suspend fun downloadVoice(voiceId: String) = withContext(Dispatchers.IO) {
        val voiceInfo = availableVoices.find { it.id == voiceId } ?: return@withContext

        val currentStates = _downloadStates.value.toMutableMap()
        currentStates[voiceId] = VoiceDownloadState.Downloading(0f)
        _downloadStates.value = currentStates

        try {
            val parts = voiceId.split("-")
            val langRegion = parts[0]
            val lang = langRegion.substringBefore("_")
            val voiceName = parts.drop(1).dropLast(1).joinToString("-")
            val quality = parts.last()

            val baseUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/$lang/$langRegion/$voiceName/$quality/"

            val modelFile = File(context.filesDir, "models/piper/$voiceId.onnx")
            val modelUrl = "${baseUrl}${voiceId}.onnx"
            downloadFile(modelUrl, modelFile) { progress ->
                val updated = _downloadStates.value.toMutableMap()
                updated[voiceId] = VoiceDownloadState.Downloading(progress)
                _downloadStates.value = updated
            }

            val configFile = File(context.filesDir, "models/piper/$voiceId.onnx.json")
            val configUrl = "${baseUrl}${voiceId}.onnx.json"
            try {
                downloadFile(configUrl, configFile) {}
            } catch (_: Exception) {}

            val updated = _downloadStates.value.toMutableMap()
            updated[voiceId] = VoiceDownloadState.Complete
            _downloadStates.value = updated
        } catch (e: Exception) {
            val updated = _downloadStates.value.toMutableMap()
            updated[voiceId] = VoiceDownloadState.Error(e.message ?: "Download failed")
            _downloadStates.value = updated
        }
    }

    fun isEspeakDataDownloaded(): Boolean {
        return File(context.filesDir, "models/piper/espeak-ng-data").exists()
    }

    suspend fun downloadEspeakData() = withContext(Dispatchers.IO) {
        val espeakDir = File(context.filesDir, "models/piper/espeak-ng-data")
        if (espeakDir.exists()) return@withContext

        val espeakUrl = ESPEAK_DATA_URL
        val zipFile = File(context.filesDir, "models/piper/espeak-ng-data.zip")

        try {
            zipFile.parentFile?.mkdirs()
            val connection = URL(espeakUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.buffered().use { input ->
                    java.io.FileOutputStream(zipFile).use { output ->
                        input.copyTo(output, bufferSize = 65536)
                    }
                }
                java.util.zip.ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val targetFile = File(espeakDir, entry.name)
                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            targetFile.parentFile?.mkdirs()
                            java.io.FileOutputStream(targetFile).use { out ->
                                zis.copyTo(out, bufferSize = 8192)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                zipFile.delete()
                AriaLogger.d("VoiceModelManager", "espeak-ng-data extracted to $espeakDir")
            }
        } catch (e: Exception) {
            AriaLogger.e("VoiceModelManager", "espeak-ng-data download failed: ${e.message}")
            zipFile.delete()
            espeakDir.deleteRecursively()
        }
    }

    suspend fun downloadCmudict() = withContext(Dispatchers.IO) {
        val dictFile = File(context.filesDir, "models/piper/cmudict.dict")
        if (dictFile.exists()) return@withContext

        try {
            dictFile.parentFile?.mkdirs()
            val connection = URL(CMUDICT_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.buffered().use { input ->
                    dictFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 65536)
                    }
                }
                AriaLogger.d("VoiceModelManager", "CMUdict downloaded to ${dictFile.absolutePath}")
            }
        } catch (e: Exception) {
            AriaLogger.e("VoiceModelManager", "CMUdict download failed: ${e.message}")
            dictFile.delete()
        }
    }

    companion object {
        private const val ESPEAK_DATA_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/espeak-ng-data.zip"
        private const val CMUDICT_URL = "https://raw.githubusercontent.com/cmusphinx/cmudict/master/cmudict.dict"

        val ALL_VOICES = listOf(
            VoiceInfo("en_US-amy-medium", "Amy", "\uD83C\uDDFA\uD83C\uDDF8", isPremium = false, isDefault = true),
            VoiceInfo("en_US-ryan-medium", "Ryan", "\uD83C\uDDFA\uD83C\uDDF8", isPremium = true),
            VoiceInfo("en_GB-alba-medium", "Alba", "\uD83C\uDDEC\uD83C\uDDE7", isPremium = true),
            VoiceInfo("en_GB-danny-low", "Danny", "\uD83C\uDDEC\uD83C\uDDE7", isPremium = true),
            VoiceInfo("en_US-lessac-high", "Lessac", "\uD83C\uDDFA\uD83C\uDDF8", isPremium = true),
            VoiceInfo("en_US-joe-medium", "Joe", "\uD83C\uDDFA\uD83C\uDDF8", isPremium = true)
        )

        fun voiceInfoForId(voiceId: String): VoiceInfo? {
            return ALL_VOICES.find { it.id == voiceId }
        }
    }

    private suspend fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: (Float) -> Unit
    ) {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) {
            AriaLogger.d("VoiceModelManager", "Voice file already cached: ${outputFile.name}")
            onProgress(1f)
            return
        }

        AriaLogger.d("VoiceModelManager", "Downloading voice: $url")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true

        val responseCode = connection.responseCode
        AriaLogger.d("VoiceModelManager", "Voice HTTP $responseCode for $url")
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
            throw java.io.FileNotFoundException("Voice file not found at $url (HTTP $responseCode)")
        }

        val totalBytes = connection.contentLengthLong
        var downloaded = 0L

        connection.inputStream.buffered().use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    downloaded += bytes
                    if (totalBytes > 0) {
                        onProgress(downloaded.toFloat() / totalBytes)
                    }
                }
            }
        }
        AriaLogger.d("VoiceModelManager", "Voice download complete: ${outputFile.name} ($downloaded bytes)")
    }
}

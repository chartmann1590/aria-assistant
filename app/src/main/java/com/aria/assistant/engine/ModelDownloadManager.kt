package com.aria.assistant.engine

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val MIN_LLM_MODEL_BYTES = 1_000_000_000L
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Float, val bytesLoaded: Long, val totalBytes: Long) : DownloadState()
        data class Complete(val modelPath: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state

    suspend fun downloadModel(url: String, filename: String) = withContext(Dispatchers.IO) {
        val outputFile = File(context.filesDir, "models/$filename")
        val partialFile = File(outputFile.parentFile, "${outputFile.name}.part")
        if (outputFile.exists() && outputFile.length() >= MIN_LLM_MODEL_BYTES) {
            AriaLogger.d("ModelDownloadManager", "Model already cached at ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            _state.value = DownloadState.Complete(outputFile.absolutePath)
            return@withContext
        }
        if (outputFile.exists()) outputFile.delete()
        outputFile.parentFile?.mkdirs()

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true

            val requestedOffset = if (partialFile.exists()) partialFile.length() else 0L
            if (requestedOffset > 0) {
                connection.setRequestProperty("Range", "bytes=$requestedOffset-")
            }

            val responseCode = connection.responseCode
            AriaLogger.d("ModelDownloadManager", "HTTP $responseCode for $url")
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                val errorMsg = "Server returned $responseCode for $url"
                AriaLogger.e("ModelDownloadManager", errorMsg)
                _state.value = DownloadState.Error(errorMsg)
                return@withContext
            }

            val existingBytes = if (responseCode == HttpURLConnection.HTTP_PARTIAL) requestedOffset else 0L
            val totalBytes = connection.contentLengthLong + existingBytes
            var downloaded = existingBytes

            connection.inputStream.buffered().use { input ->
                FileOutputStream(partialFile, existingBytes > 0).use { output ->
                    val buffer = ByteArray(65536)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        _state.value = DownloadState.Downloading(
                            progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f,
                            bytesLoaded = downloaded,
                            totalBytes = totalBytes
                        )
                    }
                }
            }
            if (partialFile.length() < MIN_LLM_MODEL_BYTES) {
                throw java.io.IOException("Downloaded model is incomplete (${partialFile.length()} bytes)")
            }
            if (!partialFile.renameTo(outputFile)) {
                throw java.io.IOException("Could not finalize downloaded model")
            }
            AriaLogger.d("ModelDownloadManager", "Download complete: ${outputFile.absolutePath} ($downloaded bytes)")
            _state.value = DownloadState.Complete(outputFile.absolutePath)
        } catch (e: Exception) {
            AriaLogger.e("ModelDownloadManager", "Download failed: ${e.message}", e)
            _state.value = DownloadState.Error(e.message ?: "Download failed: ${e::class.simpleName}")
        }
    }

    fun getCachedModelPath(filename: String): String? {
        val file = File(context.filesDir, "models/$filename")
        return if (file.exists() && file.length() >= MIN_LLM_MODEL_BYTES) file.absolutePath else null
    }

    suspend fun downloadFile(url: String, outputFile: File) = withContext(Dispatchers.IO) {
        if (outputFile.exists() && outputFile.length() > 0) return@withContext
        outputFile.parentFile?.mkdirs()
        val partialFile = File(outputFile.parentFile, "${outputFile.name}.part")
        partialFile.delete()

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw java.io.FileNotFoundException("HTTP $responseCode for $url")
        }

        connection.inputStream.buffered().use { input ->
            FileOutputStream(partialFile).use { output ->
                input.copyTo(output, bufferSize = 8192)
            }
        }
        if (partialFile.length() <= 0 || !partialFile.renameTo(outputFile)) {
            partialFile.delete()
            throw java.io.IOException("Could not finalize downloaded file")
        }
        AriaLogger.d("ModelDownloadManager", "File downloaded: ${outputFile.absolutePath}")
    }
}

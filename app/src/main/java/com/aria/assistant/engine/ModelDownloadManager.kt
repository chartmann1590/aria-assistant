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
        if (outputFile.exists() && outputFile.length() > 0) {
            AriaLogger.d("ModelDownloadManager", "Model already cached at ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            _state.value = DownloadState.Complete(outputFile.absolutePath)
            return@withContext
        }
        outputFile.parentFile?.mkdirs()

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            AriaLogger.d("ModelDownloadManager", "HTTP $responseCode for $url")
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                val errorMsg = "Server returned $responseCode for $url"
                AriaLogger.e("ModelDownloadManager", errorMsg)
                _state.value = DownloadState.Error(errorMsg)
                return@withContext
            }

            val existingBytes = if (outputFile.exists()) outputFile.length() else 0L
            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
            }

            val totalBytes = connection.contentLengthLong + existingBytes
            var downloaded = existingBytes

            connection.inputStream.buffered().use { input ->
                FileOutputStream(outputFile, existingBytes > 0).use { output ->
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
            AriaLogger.d("ModelDownloadManager", "Download complete: ${outputFile.absolutePath} ($downloaded bytes)")
            _state.value = DownloadState.Complete(outputFile.absolutePath)
        } catch (e: Exception) {
            AriaLogger.e("ModelDownloadManager", "Download failed: ${e.message}", e)
            outputFile.delete()
            _state.value = DownloadState.Error(e.message ?: "Download failed: ${e::class.simpleName}")
        }
    }

    fun getCachedModelPath(filename: String): String? {
        val file = File(context.filesDir, "models/$filename")
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    suspend fun downloadFile(url: String, outputFile: File) = withContext(Dispatchers.IO) {
        if (outputFile.exists() && outputFile.length() > 0) return@withContext
        outputFile.parentFile?.mkdirs()

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw java.io.FileNotFoundException("HTTP $responseCode for $url")
        }

        connection.inputStream.buffered().use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output, bufferSize = 8192)
            }
        }
        AriaLogger.d("ModelDownloadManager", "File downloaded: ${outputFile.absolutePath}")
    }
}

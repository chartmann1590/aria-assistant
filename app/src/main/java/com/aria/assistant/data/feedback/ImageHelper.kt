package com.aria.assistant.data.feedback

import android.content.Context
import android.net.Uri
import java.util.Base64

object ImageHelper {

    fun uriToBase64(context: Context, uri: Uri): Result<String> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                Result.success(Base64.getEncoder().encodeToString(bytes))
            } ?: Result.failure(Exception("Failed to open image stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

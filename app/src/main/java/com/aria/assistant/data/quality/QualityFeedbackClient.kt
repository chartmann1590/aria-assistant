package com.aria.assistant.data.quality

import com.aria.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class QualityFeedback(
    val category: String,
    val response: String,
    val prompt: String? = null,
    val shareContent: Boolean = false,
)

@Singleton
class QualityFeedbackClient @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean get() = BuildConfig.QUALITY_FEEDBACK_URL.isNotBlank()

    suspend fun submit(feedback: QualityFeedback): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(isConfigured) { "Quality feedback is not configured" }
            val body = buildPayload(feedback).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(BuildConfig.QUALITY_FEEDBACK_URL.trimEnd('/') + "/v1/feedback")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Feedback service returned ${response.code}" }
            }
        }
    }

    internal fun buildPayload(feedback: QualityFeedback): JSONObject = JSONObject().apply {
        put("category", feedback.category)
        put("shareContent", feedback.shareContent)
        put("appVersion", BuildConfig.VERSION_NAME)
        put("language", Locale.getDefault().toLanguageTag())
        put("responseLength", feedback.response.length)
        if (feedback.shareContent) {
            put("prompt", feedback.prompt.orEmpty())
            put("response", feedback.response)
        }
    }
}

package com.aria.assistant.data.feedback

import com.aria.assistant.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object GithubClient {

    private val baseUrl: String
        get() = BuildConfig.GITHUB_PROXY_URL.ifBlank { "https://api.github.com/" }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    val isConfigured: Boolean
        get() = (BuildConfig.GITHUB_PROXY_URL.isNotBlank() || BuildConfig.GITHUB_API_TOKEN.isNotBlank())
                && BuildConfig.GITHUB_REPO_OWNER.isNotBlank()
                && BuildConfig.GITHUB_REPO_NAME.isNotBlank()

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "Aria-Android/0.1")
                if (BuildConfig.GITHUB_API_TOKEN.isNotBlank()) {
                    request.header("Authorization", "Bearer ${BuildConfig.GITHUB_API_TOKEN}")
                }
                chain.proceed(request.build())
            }
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message ->
                if (message.contains("Authorization")) {
                    android.util.Log.d(
                        "GithubClient",
                        message.substringBefore("Authorization") +
                                "Authorization: Bearer [REDACTED]"
                    )
                } else {
                    android.util.Log.d("GithubClient", message)
                }
            }
            logging.level = HttpLoggingInterceptor.Level.HEADERS
            builder.addInterceptor(logging)
        }
        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
    }

    private val api: GithubApi by lazy {
        retrofit.create(GithubApi::class.java)
    }

    suspend fun createIssue(title: String, body: String): GithubIssue {
        return api.createIssue(
            BuildConfig.GITHUB_REPO_OWNER,
            BuildConfig.GITHUB_REPO_NAME,
            CreateIssueRequest(title = title, body = body)
        )
    }

    suspend fun getIssue(number: Int): GithubIssue {
        return api.getIssue(
            BuildConfig.GITHUB_REPO_OWNER,
            BuildConfig.GITHUB_REPO_NAME,
            number
        )
    }

    suspend fun getComments(number: Int): List<GithubComment> {
        return api.getComments(
            BuildConfig.GITHUB_REPO_OWNER,
            BuildConfig.GITHUB_REPO_NAME,
            number
        )
    }

    suspend fun postComment(number: Int, body: String): GithubComment {
        return api.postComment(
            BuildConfig.GITHUB_REPO_OWNER,
            BuildConfig.GITHUB_REPO_NAME,
            number,
            PostCommentRequest(body = body)
        )
    }

    suspend fun uploadAsset(filename: String, base64Content: String): String? {
        val request = UploadAssetRequest(
            message = "Add feedback attachment $filename",
            content = base64Content
        )
        val response = api.uploadAsset(
            BuildConfig.GITHUB_REPO_OWNER,
            BuildConfig.GITHUB_REPO_NAME,
            BuildConfig.FEEDBACK_ASSETS_DIR,
            filename,
            request
        )
        return response.content?.downloadUrl
    }
}

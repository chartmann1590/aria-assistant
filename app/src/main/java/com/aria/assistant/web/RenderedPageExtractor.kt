package com.aria.assistant.web

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class RenderedPageExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val validator = PublicUrlValidator()

    suspend fun extract(url: String, timeoutMs: Long = 12_000): String {
        if (!validator.isAllowed(url)) return ""
        return withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.Main) { loadInSandboxedWebView(url, null) }
        }.orEmpty()
    }

    internal suspend fun extractHtmlFixture(html: String, timeoutMs: Long = 5_000): String =
        withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.Main) {
                loadInSandboxedWebView("https://fixture.invalid/", html)
            }
        }.orEmpty()

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadInSandboxedWebView(url: String, fixtureHtml: String?): String =
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            val fixtureMode = fixtureHtml != null
            var completed = false

            fun finish(value: String) {
                if (completed) return
                completed = true
                webView.stopLoading()
                webView.clearHistory()
                webView.clearCache(true)
                webView.destroy()
                if (continuation.isActive) continuation.resume(value.take(MAX_RENDERED_TEXT))
            }

            fun captureBody(view: WebView) {
                if (completed) return
                view.evaluateJavascript(
                    "(function(){return document.body ? document.body.innerText : '';})()"
                ) { encoded ->
                    val decoded = runCatching {
                        JSONObject("{\"text\":$encoded}").optString("text")
                    }.getOrDefault("")
                    finish(decoded.replace(Regex("\\s+"), " ").trim())
                }
            }

            CookieManager.getInstance().setAcceptCookie(false)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
            CookieManager.getInstance().removeAllCookies(null)
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE
                setGeolocationEnabled(false)
                mediaPlaybackRequiresUserGesture = true
                saveFormData = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                safeBrowsingEnabled = true
            }
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    !fixtureMode && !validator.hasAllowedShape(request.url.toString())

                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    if (fixtureMode && request.url.host == "fixture.invalid") return null
                    val allowed = runBlocking(Dispatchers.IO) { validator.isAllowed(request.url.toString()) }
                    return if (allowed) null else blockedResponse()
                }

                override fun onPageFinished(view: WebView, loadedUrl: String) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        captureBody(view)
                    }, RENDER_SETTLE_MS)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: android.webkit.WebResourceError
                ) {
                    if (request.isForMainFrame && !fixtureMode) finish("")
                }

            }
            continuation.invokeOnCancellation {
                webView.post { finish("") }
            }
            if (fixtureHtml == null) {
                webView.loadUrl(url, mapOf("DNT" to "1"))
            } else {
                webView.loadDataWithBaseURL(url, fixtureHtml, "text/html", "utf-8", null)
                Handler(Looper.getMainLooper()).postDelayed({ captureBody(webView) }, RENDER_SETTLE_MS)
            }
        }

    private fun blockedResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain",
        "utf-8",
        ByteArrayInputStream(ByteArray(0))
    )

    private companion object {
        const val RENDER_SETTLE_MS = 1_000L
        const val MAX_RENDERED_TEXT = 12_000
    }
}

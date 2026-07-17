package com.aria.assistant.web

import com.aria.assistant.engine.AriaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebResearchService @Inject constructor(
    private val renderedPageExtractor: RenderedPageExtractor
) {
    private val validator = PublicUrlValidator()
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun research(query: String): WebResearchResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        val searchQuery = normalizeSearchQuery(query)
        val steps = mutableListOf(VerificationStep("Search", "Searching the public web for “${query.take(120)}”"))

        val candidates = linkedMapOf<String, WebSource>()
        extractDirectHttpsUrl(query)?.let { url ->
            if (validator.isAllowed(url)) {
                candidates[url] = WebSource(title = URI(url).host, url = url, snippet = "Direct URL supplied by the user")
                steps += VerificationStep("Direct page", "Validated public HTTPS URL")
            }
        }

        runCatching { searchDuckDuckGo(searchQuery) }
            .onSuccess { it.forEach { source -> candidates.putIfAbsent(source.url, source) } }
            .onFailure { AriaLogger.e("WebResearch", "DuckDuckGo search failed: ${it.message}") }

        runCatching { searchWikipedia(searchQuery) }
            .onSuccess { it.forEach { source -> candidates.putIfAbsent(source.url, source) } }
            .onFailure { AriaLogger.e("WebResearch", "Wikipedia search failed: ${it.message}") }

        val uniqueCandidates = candidates.values
            .filter { validator.hasAllowedShape(it.url) }
            .distinctBy { registrableHost(it.url) }
            .take(MAX_SOURCES)

        steps += VerificationStep("Sources", "Found ${uniqueCandidates.size} unique public sources")

        val enriched = mutableListOf<WebSource>()
        for (candidate in uniqueCandidates) {
            if (!validator.isAllowed(candidate.url)) continue
            val fetched = runCatching { fetch(candidate.url) }
                .onFailure { AriaLogger.e("WebResearch", "Page fetch failed: ${it.message}") }
                .getOrNull()
            var text = fetched?.text.orEmpty().ifBlank { candidate.extractedText }
            var rendered = false
            if (fetched?.isHtml == true && text.length < MIN_STATIC_TEXT) {
                val renderedText = runCatching { renderedPageExtractor.extract(candidate.url) }
                    .onFailure { AriaLogger.e("WebResearch", "Page render failed: ${it.message}") }
                    .getOrDefault("")
                if (renderedText.length > text.length) {
                    text = renderedText
                    rendered = true
                }
            }
            val source = candidate.copy(
                extractedText = text.take(MAX_EVIDENCE_PER_SOURCE),
                isPrimary = isPrimarySource(candidate.url),
                renderedWithJavaScript = rendered
            )
            if (source.extractedText.isNotBlank() || source.snippet.isNotBlank()) enriched += source
        }

        if (enriched.any { it.renderedWithJavaScript }) {
            steps += VerificationStep("Rendering", "Used the sandboxed JavaScript renderer for dynamic content")
        }

        val status = when {
            enriched.any { it.isPrimary && it.extractedText.isNotBlank() } -> VerificationStatus.VERIFIED
            enriched.map { registrableHost(it.url) }.distinct().size >= 2 -> VerificationStatus.VERIFIED
            enriched.isNotEmpty() -> VerificationStatus.PARTIAL
            else -> VerificationStatus.UNAVAILABLE
        }
        val warning = when (status) {
            VerificationStatus.PARTIAL -> "Only one usable non-primary source was available; treat the answer as partially verified."
            VerificationStatus.UNAVAILABLE -> "Web verification could not be completed. The answer may rely on local model knowledge."
            else -> null
        }
        steps += VerificationStep("Cross-check", when (status) {
            VerificationStatus.VERIFIED -> "Evidence threshold met"
            VerificationStatus.PARTIAL -> "Evidence threshold not fully met"
            VerificationStatus.UNAVAILABLE -> "No usable web evidence"
            else -> status.name.lowercase()
        })

        WebResearchResult(
            VerificationTrace(
                query = query,
                status = status,
                sources = enriched,
                steps = steps,
                retrievedAt = System.currentTimeMillis(),
                elapsedMs = System.currentTimeMillis() - started,
                warning = warning
            )
        )
    }

    private suspend fun searchDuckDuckGo(query: String): List<WebSource> {
        val sources = mutableListOf<WebSource>()
        val instantUrl = "https://api.duckduckgo.com/".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .addQueryParameter("no_html", "1")
            .addQueryParameter("skip_disambig", "1")
            .build().toString()
        fetchRaw(instantUrl)?.let { raw ->
            runCatching {
                val json = JSONObject(raw)
                val abstract = json.optString("AbstractText").trim()
                val url = json.optString("AbstractURL").trim()
                if (abstract.length > 40 && validator.hasAllowedShape(url)) {
                    sources += WebSource(
                        title = json.optString("Heading").ifBlank { "DuckDuckGo Instant Answer" },
                        url = url,
                        snippet = abstract.take(500)
                    )
                }
            }
        }

        val searchUrl = "https://html.duckduckgo.com/html/".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .build().toString()
        val html = fetchRaw(searchUrl) ?: return sources
        val document = Jsoup.parse(html)
        document.select(".result").take(8).forEach { result ->
            val link = result.selectFirst("a.result__a") ?: return@forEach
            val resolved = resolveDuckDuckGoUrl(link.attr("href"))
            if (!validator.hasAllowedShape(resolved)) return@forEach
            val snippet = result.selectFirst(".result__snippet")?.text().orEmpty()
            sources += WebSource(link.text().ifBlank { URI(resolved).host }, resolved, snippet.take(500))
        }
        return sources
    }

    private suspend fun searchWikipedia(query: String): List<WebSource> {
        val url = "https://en.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrsearch", query)
            .addQueryParameter("gsrlimit", "2")
            .addQueryParameter("prop", "extracts|info")
            .addQueryParameter("exintro", "1")
            .addQueryParameter("explaintext", "1")
            .addQueryParameter("inprop", "url")
            .addQueryParameter("format", "json")
            .build().toString()
        val raw = fetchRaw(url) ?: return emptyList()
        val pages = JSONObject(raw).optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
        return pages.keys().asSequence().mapNotNull { key ->
            val page = pages.optJSONObject(key) ?: return@mapNotNull null
            val pageUrl = page.optString("fullurl")
            if (!validator.hasAllowedShape(pageUrl)) return@mapNotNull null
            WebSource(
                title = page.optString("title"),
                url = pageUrl,
                snippet = page.optString("extract").take(500),
                extractedText = page.optString("extract").take(MAX_EVIDENCE_PER_SOURCE)
            )
        }.toList()
    }

    private suspend fun fetch(url: String): FetchedPage? {
        val raw = fetchRawResponse(url) ?: return null
        val text = if (raw.isHtml) extractReadableText(raw.body) else raw.body.replace(Regex("\\s+"), " ").trim()
        return FetchedPage(text, raw.isHtml)
    }

    private suspend fun fetchRaw(url: String): String? = fetchRawResponse(url)?.body

    private suspend fun fetchRawResponse(initialUrl: String): RawResponse? {
        var current = initialUrl
        repeat(MAX_REDIRECTS + 1) {
            if (!validator.isAllowed(current)) return null
            val request = Request.Builder()
                .url(current)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/json,text/plain;q=0.9,*/*;q=0.5")
                .header("Accept-Language", "en-US,en;q=0.8")
                .header("DNT", "1")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code in 300..399) {
                    val next = response.header("Location") ?: return null
                    current = response.request.url.resolve(next)?.toString() ?: return null
                    return@repeat
                }
                if (!response.isSuccessful) return null
                val body = response.body ?: return null
                val length = body.contentLength()
                if (length > MAX_RESPONSE_BYTES) return null
                val bytes = body.byteStream().use { input ->
                    val buffer = ByteArray(MAX_RESPONSE_BYTES.toInt() + 1)
                    var total = 0
                    while (total < buffer.size) {
                        val read = input.read(buffer, total, buffer.size - total)
                        if (read < 0) break
                        total += read
                    }
                    if (total > MAX_RESPONSE_BYTES) return null
                    buffer.copyOf(total)
                }
                val contentType = body.contentType()?.toString().orEmpty().lowercase()
                if (contentType.isNotBlank() && NONE_TEXT_TYPES.any { contentType.startsWith(it) }) return null
                return RawResponse(
                    body = bytes.toString(Charsets.UTF_8),
                    isHtml = contentType.contains("html") ||
                        bytes.copyOfRange(0, minOf(80, bytes.size)).toString(Charsets.UTF_8).contains("<html", true)
                )
            }
        }
        return null
    }

    private fun extractReadableText(html: String): String {
        val document = Jsoup.parse(html)
        document.select("script,style,noscript,nav,header,footer,aside,form,svg,canvas").remove()
        return (document.selectFirst("article")?.text()
            ?: document.selectFirst("main")?.text()
            ?: document.body()?.text().orEmpty())
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_EVIDENCE_PER_SOURCE)
    }

    private fun resolveDuckDuckGoUrl(raw: String): String {
        val absolute = when {
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("/") -> "https://duckduckgo.com$raw"
            else -> raw
        }
        return runCatching {
            val uri = URI(absolute)
            val encoded = uri.rawQuery.orEmpty().split('&')
                .firstOrNull { it.startsWith("uddg=") }
                ?.substringAfter('=')
            if (encoded != null) URLDecoder.decode(encoded, "UTF-8") else absolute
        }.getOrDefault("")
    }

    private fun extractDirectHttpsUrl(text: String): String? =
        Regex("https://[^\\s<>()]+", RegexOption.IGNORE_CASE).find(text)?.value?.trimEnd('.', ',', ';', ')')

    private fun registrableHost(url: String): String = runCatching {
        val host = URI(url).host.removePrefix("www.").lowercase()
        host.split('.').takeLast(2).joinToString(".")
    }.getOrDefault(url)

    private fun isPrimarySource(url: String): Boolean = runCatching {
        val host = URI(url).host.lowercase()
        host.endsWith(".gov") || host.endsWith(".mil") || host == "who.int" || host.endsWith(".who.int")
    }.getOrDefault(false)

    private data class FetchedPage(val text: String, val isHtml: Boolean)
    private data class RawResponse(val body: String, val isHtml: Boolean)

    internal companion object {
        const val MAX_SOURCES = 4
        const val MAX_REDIRECTS = 4
        const val MAX_RESPONSE_BYTES = 2_000_000L
        const val MAX_EVIDENCE_PER_SOURCE = 6_000
        const val MIN_STATIC_TEXT = 280
        const val USER_AGENT = "AriaAssistant/1.0 (Android; public web verification)"
        val NONE_TEXT_TYPES = listOf("image/", "audio/", "video/", "application/zip", "application/octet-stream")

        fun normalizeSearchQuery(query: String): String {
            val cleaned = query
                .replace(Regex("(?i)\\b(?:please\\s+)?(?:verify|check|confirm)(?:\\s+(?:this|it))?\\s+(?:on|using|with)\\s+the\\s+web\\b[.!?]*"), " ")
                .replace(Regex("(?i)\\b(?:search|look\\s+it\\s+up)\\s+(?:on|using)\\s+the\\s+web\\b[.!?]*"), " ")
                .replace(Regex("\\s+"), " ")
                .trim(' ', '.', ',', ';', ':')
            return cleaned.ifBlank { query.trim() }
        }
    }
}

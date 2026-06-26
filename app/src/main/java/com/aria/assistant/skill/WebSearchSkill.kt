package com.aria.assistant.skill

import com.aria.assistant.engine.AriaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.aria.assistant.domain.model.SearchResultItem
import com.aria.assistant.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSearchSkill @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

    private val timeoutMs = 12000

    var lastSearchCards: List<SearchResultItem> = emptyList()
        private set

    suspend fun search(query: String): SkillResult<String> = withContext(Dispatchers.IO) {
        AriaLogger.d("WebSearchSkill", "Search: $query")
        lastSearchCards = emptyList()
        val lower = query.lowercase()

        if (lower.contains("my ip") || lower.contains("my ip address") || lower.contains("what is my ip")) {
            val ip = lookupPublicIp()
            if (ip.isNotBlank()) return@withContext SkillResult.Success("Your public IP address is $ip.")
        }

        if (lower.contains("weather") || lower.contains("forecast") || lower.contains("temperature")) {
            val loc = extractLocationAny(query)
            if (loc != null) {
                AriaLogger.d("WebSearchSkill", "Weather lookup: $loc")
                val weather = getWeather(loc)
                if (weather.isNotBlank()) return@withContext SkillResult.Success(weather)
            }
        }

        val ddgApi = searchDuckDuckGoApi(query)
        if (ddgApi.isNotBlank()) {
            AriaLogger.d("WebSearchSkill", "DDG API: ${ddgApi.length} chars")
            return@withContext SkillResult.Success(ddgApi)
        }

        val wiki = searchWikipedia(query)
        if (wiki.isNotBlank()) {
            AriaLogger.d("WebSearchSkill", "Wikipedia: ${wiki.length} chars")
            return@withContext SkillResult.Success(wiki)
        }

        val ddgResult = searchDuckDuckGoWithContent(query)
        if (ddgResult.isNotBlank()) {
            AriaLogger.d("WebSearchSkill", "DDG+crawl: ${ddgResult.length} chars")
            return@withContext SkillResult.Success(ddgResult)
        }

        AriaLogger.d("WebSearchSkill", "No results")
        return@withContext SkillResult.Success("")
    }

    private suspend fun searchDuckDuckGoWithContent(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val html = httpGet("https://html.duckduckgo.com/html/?q=$encoded", null) ?: return ""
        if (isCaptcha(html)) return ""

        val results = parseDdgResultsWithUrls(html)
        if (results.isEmpty()) return ""
        lastSearchCards = results.take(5)

        val parts = mutableListOf<String>()
        parts.add("Search results:\n${results.joinToString("\n").take(800)}")

        val topUrl = results.firstOrNull()?.url
        if (topUrl != null && topUrl.isNotBlank() && topUrl.startsWith("http")) {
            val pageContent = fetchPageContent(topUrl)
            if (pageContent.isNotBlank()) {
                parts.add("\n\nPage content:\n${pageContent.take(2000)}")
            }
        }

        val secondUrl = results.getOrNull(1)?.url
        if (secondUrl != null && secondUrl.isNotBlank() && results.size > 1 && secondUrl.startsWith("http") && secondUrl != topUrl) {
            val pageContent2 = fetchPageContent(secondUrl)
            if (pageContent2.isNotBlank()) {
                parts.add("\n\nMore content:\n${pageContent2.take(1000)}")
            }
        }

        return parts.joinToString("\n")
    }

    private fun parseDdgResultsWithUrls(html: String): List<SearchResultItem> {
        val results = mutableListOf<SearchResultItem>()

        val linkRegex = Regex("""<a\s[^>]*class=['"]result__a['"][^>]*href="([^"]+)"[^>]*>([^<]+)</a>""", RegexOption.IGNORE_CASE)
        val snippetRegex = Regex("""<a\s[^>]*class=['"]result__snippet['"][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

        val links = linkRegex.findAll(html).toList()
        val snippets = snippetRegex.findAll(html).map { stripHtml(it.groupValues[1]).trim() }.toList()

        for (i in links.indices) {
            val rawHref = links[i].groupValues[1]
            val title = decodeHtmlEntities(links[i].groupValues[2].trim())
            if (title.length < 3) continue

            val resolvedUrl = resolveDdgUrl(rawHref)
            val snippet = snippets.getOrNull(i)?.take(200) ?: ""

            results.add(SearchResultItem(title, snippet, resolvedUrl))
            if (results.size >= 5) break
        }

        return results
    }

    private fun resolveDdgUrl(rawHref: String): String {
        val uddgMatch = Regex("""uddg=([^&]+)""").find(rawHref)
        if (uddgMatch != null) {
            return try {
                val decoded = java.net.URLDecoder.decode(uddgMatch.groupValues[1], "UTF-8")
                if (decoded.contains("duckduckgo.com/y.js") || decoded.contains("bing.com/aclick") || decoded.length > 500) {
                    ""
                } else {
                    decoded
                }
            } catch (e: Exception) {
                ""
            }
        }
        return if (rawHref.startsWith("//")) "https:$rawHref" else rawHref
    }

    private suspend fun fetchPageContent(urlString: String): String = withContext(Dispatchers.IO) {
        try {
            val html = httpGet(urlString, null) ?: return@withContext ""
            extractReadableText(html)
        } catch (e: Exception) {
            AriaLogger.e("WebSearchSkill", "Page fetch error: ${e.message}")
            ""
        }
    }

    private fun extractReadableText(html: String): String {
        var text = html
            .replace(Regex("""<script[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<style[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<nav[^>]*>.*?</nav>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<header[^>]*>.*?</header>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<footer[^>]*>.*?</footer>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<aside[^>]*>.*?</aside>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<noscript[^>]*>.*?</noscript>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<form[^>]*>.*?</form>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<iframe[^>]*>.*?</iframe>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<svg[^>]*>.*?</svg>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""&[a-z]+;"""), " ")
            .replace(Regex("""&#\d+;"""), " ")
            .replace(Regex("""https?://\S+"""), " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

        text = text.replace(Regex("""\b(?:cookies|privacy policy|subscribe|newsletter|advertisement|sponsored|click here|read more|share this|comments|log in|sign up|accept all|reject all)\b[^.]*\.?""", RegexOption.IGNORE_CASE), "")

        return text.take(3000)
    }

    private fun isCaptcha(html: String): Boolean {
        return html.contains("g-recaptcha") ||
            html.contains("verify you are human") ||
            html.contains("action=\"/challenge\"")
    }

    private fun lookupPublicIp(): String {
        return try {
            val text = httpGet("https://api.ipify.org", null) ?: return ""
            text.trim()
        } catch (e: Exception) {
            AriaLogger.e("WebSearchSkill", "IP lookup failed: ${e.message}")
            ""
        }
    }

    private fun extractLocationAny(query: String): String? {
        var loc = query

        loc = loc.replace(Regex("""^(?:could you|could you please|can you|would you|please|do you know|i want to know|tell me|show me|find|check|look up|search for|what's the|whats the|what is the|how's the|hows the|what|how|is|will|can|could|would)\s+""", RegexOption.IGNORE_CASE), "")

        loc = loc.replace(Regex("""\b(?:weather|forecast|temperature|temp)\b""", RegexOption.IGNORE_CASE), " ")

        loc = loc.replace(Regex("""\s+(?:next|this)\s+(?:weekend|week|month|year)|(?:\s+(?:today|tomorrow|now|right now|currently|going to be|gonna be|be like|look like|the weather|forecast|report))\s*$""", RegexOption.IGNORE_CASE), "")

        loc = loc.replace(Regex("""\b(?:the|a|an|in|at|for|like|going|to|gonna|be|will|is|are|there|it|outside|please|tell|me)\b""", RegexOption.IGNORE_CASE), " ")

        loc = loc.replace(Regex("""[?.!,;:]"""), " ")
        loc = loc.replace(Regex("""\s+"""), " ").trim()

        return loc.ifBlank { null }
    }

    private suspend fun getWeather(location: String): String {
        return try {
            val isFahrenheit = settingsRepository.getVoiceConfig().first().temperatureUnit == "fahrenheit"
            val unitParam = if (isFahrenheit) "u" else "m"
            val tempLabel = if (isFahrenheit) "\u00b0F" else "\u00b0C"
            val encoded = URLEncoder.encode(location.trim(), "UTF-8")

            val currentText = httpGet("https://wttr.in/$encoded?format=%C+%t+%w+%h&$unitParam", "curl/8.4.0")
            if (currentText == null || currentText.length > 200 || currentText.contains("DOCTYPE", true) || currentText.contains("<html", true)) {
                AriaLogger.d("WebSearchSkill", "Current weather rejected")
                return ""
            }
            val current = currentText.trim().replace(Regex("\\s+"), " ")

            val forecastJson = httpGet("https://wttr.in/$encoded?format=j1", "curl/8.4.0")
            val forecast = if (forecastJson != null) parseWttrForecast(forecastJson, isFahrenheit) else ""

            val parts = mutableListOf("Weather in $location: $current")
            if (forecast.isNotBlank()) parts.add("Forecast: $forecast")
            parts.joinToString(". ")
        } catch (e: Exception) {
            AriaLogger.e("WebSearchSkill", "Weather lookup failed: ${e.message}")
            ""
        }
    }

    private fun parseWttrForecast(jsonStr: String, fahrenheit: Boolean): String {
        return try {
            val root = JSONObject(jsonStr)
            val weather = root.optJSONArray("weather") ?: return ""
            val parts = mutableListOf<String>()

            for (i in 0 until minOf(weather.length(), 3)) {
                val day = weather.optJSONObject(i) ?: continue
                val date = day.optString("date", "")
                val maxKey = if (fahrenheit) "maxtempF" else "maxtempC"
                val minKey = if (fahrenheit) "mintempF" else "mintempC"
                val max = day.optString(maxKey, "")
                val min = day.optString(minKey, "")
                val hourly = day.optJSONArray("hourly")
                var condition = ""
                if (hourly != null && hourly.length() > 0) {
                    val noon = hourly.optJSONObject(minOf(4, hourly.length() - 1))
                    if (noon != null) {
                        val desc = noon.optJSONArray("weatherDesc")
                        if (desc != null && desc.length() > 0) {
                            condition = desc.optJSONObject(0)?.optString("value", "") ?: ""
                        }
                    }
                }
                val dayLabel = if (i == 0) "Today" else if (i == 1) "Tomorrow" else date.takeLast(5)
                val line = buildString {
                    append("$dayLabel: $condition".trim())
                    if (max.isNotBlank() && min.isNotBlank()) append(" $min\u00b0-$max\u00b0")
                }
                parts.add(line)
            }

            parts.joinToString("; ")
        } catch (e: Exception) {
            ""
        }
    }

    private fun searchDuckDuckGoApi(query: String): String {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val json = httpGet("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1", null) ?: return ""
            val obj = JSONObject(json)
            val parts = mutableListOf<String>()

            val heading = obj.optString("Heading", "").trim()
            val abstract = obj.optString("AbstractText", "").trim().take(500)
            val source = obj.optString("AbstractSource", "").trim()

            if (abstract.length > 15) {
                val entry = buildString {
                    if (heading.isNotBlank()) append("$heading: ")
                    append(abstract)
                    if (source.isNotBlank()) append(" (via $source)")
                }
                parts.add(entry)
            }

            val relatedTopics = obj.optJSONArray("RelatedTopics")
            if (relatedTopics != null) {
                for (i in 0 until minOf(relatedTopics.length(), 3)) {
                    val topic = relatedTopics.optJSONObject(i)
                    if (topic != null) {
                        val text = topic.optString("Text", "").trim().take(300)
                        if (text.length > 15) {
                            parts.add(decodeHtmlEntities(text))
                        }
                    }
                }
            }

            parts.joinToString("\n").take(600).ifBlank { "" }
        } catch (e: Exception) {
            AriaLogger.e("WebSearchSkill", "DDG API error: ${e.message}")
            ""
        }
    }

    private fun searchWikipedia(query: String): String {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=3&format=json"
            val searchJson = httpGet(searchUrl, null) ?: return ""

            val arr = JSONArray(searchJson)
            if (arr.length() < 2) return ""
            val titles = arr.optJSONArray(1) ?: return ""

            val parts = mutableListOf<String>()
            for (i in 0 until minOf(titles.length(), 2)) {
                val title = titles.optString(i, "").trim()
                if (title.isBlank()) continue
                val summary = getWikipediaSummary(title)
                if (summary.isNotBlank()) parts.add(summary)
            }

            if (parts.isEmpty()) {
                val simpler = query
                    .replace(Regex("^(what|who|when|where|how|why|is|are|does|do|did|can|tell me|show me|find|search for)\\s+", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (simpler != query && simpler.isNotBlank()) {
                    return searchWikipedia(simpler)
                }
            }

            parts.joinToString("\n\n").take(600)
        } catch (e: Exception) {
            AriaLogger.e("WebSearchSkill", "Wikipedia error: ${e.javaClass.simpleName}: ${e.message}")
            ""
        }
    }

    private fun getWikipediaSummary(title: String): String {
        return try {
            val encoded = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
            val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$encoded"
            val json = httpGet(url, null) ?: return ""
            val obj = JSONObject(json)
            obj.optString("extract", "").trim().take(400)
        } catch (e: Exception) {
            ""
        }
    }

    private fun httpGet(urlString: String, userAgent: String?): String? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                val ua = userAgent ?: "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                setRequestProperty("User-Agent", ua)
                setRequestProperty("Accept", "text/html,application/json,*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                instanceFollowRedirects = true
            }
            val responseCode = conn.responseCode
            if (responseCode != 200) {
                AriaLogger.d("WebSearchSkill", "HTTP $responseCode for ${urlString.take(80)}")
                conn.disconnect()
                return null
            }
            val bytes = conn.inputStream.readBytes()
            val text = String(bytes, Charsets.UTF_8)
            conn.disconnect()
            text
        } catch (e: Exception) {
            AriaLogger.e("WebSearchSkill", "HTTP error: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&nbsp;", " ")
            .replace("&#x2F;", "/")
            .replace(Regex("&#(\\d+);")) { mr -> mr.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: mr.value }
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

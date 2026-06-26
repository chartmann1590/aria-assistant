package com.aria.assistant.skill

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TestReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timeoutMs = 12000

    override fun onReceive(context: Context, intent: Intent) {
        val query = intent.getStringExtra("query") ?: return
        val action = intent.getStringExtra("action") ?: "default"
        Log.w("Aria", "=== TEST: $query (action=$action) ===")
        
        scope.launch {
            when (action) {
                "weather" -> testWeather(query)
                "ddg_lite_html" -> testDdgLiteHtml(query)
                "ddg_html_html" -> testDdgHtmlHtml(query)
                "ddg_lite_parse" -> testDdgLiteParse(query)
                "ddg_html_parse" -> testDdgHtmlParse(query)
                "wiki_summary" -> testWikiSummary(query)
                else -> testAll(query)
            }
        }
    }

    private fun testAll(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        
        // Weather
        Log.w("Aria", "=== WEATHER ===")
        val weatherText = httpGet("https://wttr.in/$encoded?format=4", "curl/8.4.0")
        Log.w("Aria", "Weather (curl UA): ${weatherText?.take(500)}")
        
        // DDG API
        Log.w("Aria", "=== DDG API ===")
        val ddgApiText = httpGet("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1", null)
        Log.w("Aria", "DDG API: ${ddgApiText?.take(400)}")
        
        // DDG Lite
        Log.w("Aria", "=== DDG LITE ===")
        val liteHtml = httpGet("https://lite.duckduckgo.com/lite/?q=$encoded", null)
        if (liteHtml != null) {
            val isCaptcha = liteHtml.contains("g-recaptcha") || liteHtml.contains("verify you are human")
            Log.w("Aria", "DDG Lite real captcha: $isCaptcha, len=${liteHtml.length}")
            
            // Show link structure
            val linkPattern = Regex("""<a[^>]*class="result-link"[^>]*>([^<]+)</a>""")
            val links = linkPattern.findAll(liteHtml).take(5).map { it.groupValues[1].trim() }.toList()
            Log.w("Aria", "DDG Lite links: $links")
            
            // Show surrounding HTML for first result
            val firstLinkIdx = liteHtml.indexOf("result-link")
            if (firstLinkIdx > 0) {
                val snippet = liteHtml.substring(maxOf(0, firstLinkIdx - 50), minOf(liteHtml.length, firstLinkIdx + 500))
                Log.w("Aria", "DDG Lite around 1st link: $snippet")
            }
        }
        
        // DDG HTML
        Log.w("Aria", "=== DDG HTML ===")
        val htmlHtml = httpGet("https://html.duckduckgo.com/html/?q=$encoded", null)
        if (htmlHtml != null) {
            val isCaptcha = htmlHtml.contains("g-recaptcha") || htmlHtml.contains("verify you are human")
            Log.w("Aria", "DDG HTML real captcha: $isCaptcha, len=${htmlHtml.length}")
            
            val linkPattern = Regex("""<a[^>]*class="result__a"[^>]*>([^<]+)</a>""")
            val links = linkPattern.findAll(htmlHtml).take(5).map { it.groupValues[1].trim() }.toList()
            Log.w("Aria", "DDG HTML result__a links: $links")
            
            val snippetPattern = Regex("""<a[^>]*class="result__snippet"[^>]*>([^<]+)</a>""")
            val snippets = snippetPattern.findAll(htmlHtml).take(5).map { it.groupValues[1].trim() }.toList()
            Log.w("Aria", "DDG HTML result__snippet: $snippets")
            
            val firstIdx = htmlHtml.indexOf("result__a")
            if (firstIdx > 0) {
                val snippet = htmlHtml.substring(maxOf(0, firstIdx - 50), minOf(htmlHtml.length, firstIdx + 800))
                Log.w("Aria", "DDG HTML around 1st link: $snippet")
            }
        }
        
        // Wikipedia
        Log.w("Aria", "=== WIKIPEDIA ===")
        val wikiJson = httpGet("https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=3&format=json", null)
        Log.w("Aria", "Wiki: ${wikiJson?.take(300)}")
        
        Log.w("Aria", "=== DONE ===")
    }
    
    private fun testWeather(loc: String) {
        val encoded = URLEncoder.encode(loc, "UTF-8")
        Log.w("Aria", "--- Weather with curl UA ---")
        val t1 = httpGet("https://wttr.in/$encoded?format=%C+%t+%w+%h&m", "curl/8.4.0")
        Log.w("Aria", "curl UA: ${t1?.take(200)}")
        
        Log.w("Aria", "--- Weather with format=4 ---")
        val t2 = httpGet("https://wttr.in/$encoded?format=4", "curl/8.4.0")
        Log.w("Aria", "format=4: ${t2?.take(200)}")
        
        Log.w("Aria", "--- Weather format=j1 ---")
        val t3 = httpGet("https://wttr.in/$encoded?format=j1", "curl/8.4.0")
        Log.w("Aria", "j1: ${t3?.take(500)}")
    }
    
    private fun testDdgLiteHtml(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val html = httpGet("https://lite.duckduckgo.com/lite/?q=$encoded", null)
        if (html != null) {
            Log.w("Aria", "DDG Lite full (${html.length} chars):")
            Log.w("Aria", html)
        }
    }
    
    private fun testDdgHtmlHtml(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val html = httpGet("https://html.duckduckgo.com/html/?q=$encoded", null)
        if (html != null) {
            Log.w("Aria", "DDG HTML full (${html.length} chars):")
            Log.w("Aria", html)
        }
    }
    
    private fun testDdgLiteParse(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val html = httpGet("https://lite.duckduckgo.com/lite/?q=$encoded", null) ?: return
        
        val linkRegex = Regex("""<a[^>]*class="result-link"[^>]*>([^<]+)</a>""")
        val snippetRegex = Regex("""<(?:span|td)[^>]*class="result-snippet"[^>]*>\s*([^<]*?)\s*</(?:span|td)>""")
        val descRegex = Regex("""<td[^>]*class="result-snippet"[^>]*>(.*?)</td>""", RegexOption.DOT_MATCHES_ALL)
        
        val links = linkRegex.findAll(html).map { it.groupValues[1].trim() }.toList()
        val snippets = snippetRegex.findAll(html).map { it.groupValues[1].trim() }.toList()
        val descs = descRegex.findAll(html).map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }.toList()
        
        Log.w("Aria", "DDG Lite parse test:")
        Log.w("Aria", "  Links (${links.size}): $links")
        Log.w("Aria", "  Snippets (${snippets.size}): $snippets")
        Log.w("Aria", "  Descs (${descs.size}): $descs")
    }
    
    private fun testDdgHtmlParse(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val html = httpGet("https://html.duckduckgo.com/html/?q=$encoded", null) ?: return
        
        val linkRegex = Regex("""<a[^>]*class="result__a"[^>]*>([^<]+)</a>""")
        val urlRegex = Regex("""<a[^>]*class="result__url"[^>]*>([^<]+)</a>""")
        val snippetRegex = Regex("""<a[^>]*class="result__snippet"[^>]*>((?:(?!</a>).)*)</a>""")
        
        val links = linkRegex.findAll(html).map { it.groupValues[1].trim() }.toList()
        val urls = urlRegex.findAll(html).map { it.groupValues[1].trim() }.toList()
        val snippets = snippetRegex.findAll(html).map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }.toList()
        
        Log.w("Aria", "DDG HTML parse test:")
        Log.w("Aria", "  Links (${links.size}): $links")
        Log.w("Aria", "  URLs (${urls.size}): $urls")
        Log.w("Aria", "  Snippets (${snippets.size}): $snippets")
    }
    
    private fun testWikiSummary(title: String) {
        val encoded = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
        val json = httpGet("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded", null)
        Log.w("Aria", "Wiki summary for '$title': ${json?.take(500)}")
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
            val code = conn.responseCode
            Log.w("Aria", "  HTTP $code: ${urlString.take(100)}")
            if (code != 200) { conn.disconnect(); return null }
            val bytes = conn.inputStream.readBytes()
            val text = String(bytes, Charsets.UTF_8)
            conn.disconnect()
            text
        } catch (e: Exception) {
            Log.w("Aria", "  HTTP FAIL: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
}

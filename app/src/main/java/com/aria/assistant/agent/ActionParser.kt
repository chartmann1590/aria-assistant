package com.aria.assistant.agent

import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionParser @Inject constructor() {

    private val actionRegex = Regex("<action>(.*?)</action>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val sayRegex = Regex("<say>(.*?)</say>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    fun extractAction(text: String): JSONObject? {
        val match = actionRegex.find(text) ?: return extractJsonBlock(text)
        return try {
            JSONObject(match.groupValues[1].trim())
        } catch (_: Exception) {
            null
        }
    }

    fun extractSay(text: String): String? {
        val match = sayRegex.find(text)
        if (match != null) {
            val content = match.groupValues[1].trim()
            if (content.isNotBlank()) return content
        }
        if (!text.contains("<action>") && !text.contains("<say>")) {
            val json = extractJsonBlock(text)
            if (json == null) {
                val cleaned = text.trim()
                if (cleaned.isNotBlank()) return cleaned
            }
        }
        return null
    }

    private fun extractJsonBlock(text: String): JSONObject? {
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return try {
                            JSONObject(text.substring(start, i + 1))
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }
        }
        return null
    }
}

package com.aria.assistant.web

import org.json.JSONArray
import org.json.JSONObject

object WebVerificationMetadata {
    private const val TYPE = "web_verification"
    private const val VERSION = 1

    fun encode(trace: VerificationTrace): String = JSONObject().apply {
        put("type", TYPE)
        put("version", VERSION)
        put("query", trace.query)
        put("status", trace.status.name)
        put("retrieved_at", trace.retrievedAt)
        put("elapsed_ms", trace.elapsedMs)
        trace.warning?.let { put("warning", it) }
        put("steps", JSONArray().apply {
            trace.steps.forEach { step ->
                put(JSONObject().put("label", step.label).put("detail", step.detail))
            }
        })
        put("sources", JSONArray().apply {
            trace.sources.forEach { source ->
                put(JSONObject().apply {
                    put("title", source.title)
                    put("url", source.url)
                    put("snippet", source.snippet.take(300))
                    put("primary", source.isPrimary)
                    put("javascript", source.renderedWithJavaScript)
                })
            }
        })
    }.toString()

    fun decode(metadata: String?): VerificationTrace? {
        if (metadata.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(metadata)
            if (json.optString("type") != TYPE) return null
            val sourcesJson = json.optJSONArray("sources") ?: JSONArray()
            val sources = (0 until sourcesJson.length()).mapNotNull { index ->
                sourcesJson.optJSONObject(index)?.let { source ->
                    WebSource(
                        title = source.optString("title"),
                        url = source.optString("url"),
                        snippet = source.optString("snippet"),
                        isPrimary = source.optBoolean("primary"),
                        renderedWithJavaScript = source.optBoolean("javascript")
                    )
                }
            }
            val stepsJson = json.optJSONArray("steps") ?: JSONArray()
            val steps = (0 until stepsJson.length()).mapNotNull { index ->
                stepsJson.optJSONObject(index)?.let { step ->
                    VerificationStep(step.optString("label"), step.optString("detail"))
                }
            }
            VerificationTrace(
                query = json.optString("query"),
                status = runCatching { VerificationStatus.valueOf(json.optString("status")) }
                    .getOrDefault(VerificationStatus.UNAVAILABLE),
                sources = sources,
                steps = steps,
                retrievedAt = json.optLong("retrieved_at"),
                elapsedMs = json.optLong("elapsed_ms"),
                warning = json.optString("warning").ifBlank { null }
            )
        }.getOrNull()
    }
}

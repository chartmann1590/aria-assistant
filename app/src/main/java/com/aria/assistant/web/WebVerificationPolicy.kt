package com.aria.assistant.web

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebVerificationPolicy @Inject constructor() {

    fun shouldVerify(text: String, mode: WebVerificationMode): Boolean {
        val normalized = text.trim().lowercase()
        if (normalized.isBlank()) return false
        val explicit = EXPLICIT_WEB.containsMatchIn(normalized)
        if (explicit) return true
        if (mode == WebVerificationMode.EXPLICIT_ONLY) return false
        if (isClearlyLocalOrNonFactual(normalized)) return false
        if (mode == WebVerificationMode.CURRENT_ONLY) return CURRENT_FACTS.containsMatchIn(normalized)
        return FACTUAL_SHAPE.containsMatchIn(normalized) || normalized.length >= 18
    }

    private fun isClearlyLocalOrNonFactual(text: String): Boolean =
        CASUAL.matches(text) ||
            CREATIVE.containsMatchIn(text) ||
            DEVICE_ACTION.containsMatchIn(text) ||
            PERSONAL_DATA.containsMatchIn(text)

    private companion object {
        val EXPLICIT_WEB = Regex("\\b(search|browse|look up|lookup|verify|check online|on the web|internet)\\b")
        val CURRENT_FACTS = Regex(
            "\\b(today|tonight|tomorrow|current|currently|latest|recent|news|weather|forecast|price|stock|score|schedule|open now|hours|president|ceo|version|release|election|recommend|best|nearby)\\b"
        )
        val FACTUAL_SHAPE = Regex(
            "^(who|what|when|where|why|how|which|is|are|was|were|does|do|did|can|could|tell me about|explain|compare|recommend)\\b"
        )
        val DEVICE_ACTION = Regex(
            "^(set|start|cancel|stop|open|launch|call|text|message|send|reply|dismiss|turn|switch|increase|decrease|lower|raise|take a photo|read my|show my|navigate|play|pause|skip|copy|paste|email)\\b"
        )
        val PERSONAL_DATA = Regex("\\b(my notifications|my messages|my sms|my calendar|my contacts|my battery|my storage|my clipboard|my screen|my last call)\\b")
        val CREATIVE = Regex("^(write|draft|compose|create|make up|brainstorm|roleplay|tell)\\b.*\\b(poem|story|joke|song|letter|email|message|ideas|fiction)\\b")
        val CASUAL = Regex("^(hi|hello|hey|thanks|thank you|good morning|good afternoon|good evening|bye|goodbye)[!. ]*$")
    }
}

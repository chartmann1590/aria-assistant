package com.aria.assistant.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebResearchServiceTest {
    @Test
    fun `search query removes web verification instructions`() {
        assertEquals(
            "Latest Android version",
            WebResearchService.normalizeSearchQuery("Latest Android version verify on the web")
        )
        assertEquals(
            "Who is the current president of France?",
            WebResearchService.normalizeSearchQuery("Who is the current president of France? Please check it using the web.")
        )
    }

    @Test
    fun `prompt evidence keeps search summary before page excerpt`() {
        val evidence = WebResearchResult(
            VerificationTrace(
                query = "latest Android version",
                status = VerificationStatus.VERIFIED,
                sources = listOf(WebSource("Android", "https://example.com", "Android 16 is current", "navigation text")),
                steps = emptyList(),
                retrievedAt = 1,
                elapsedMs = 1
            )
        ).toPromptEvidence()
        assertTrue(evidence.indexOf("SEARCH SUMMARY") < evidence.indexOf("PAGE EXCERPT"))
    }
}

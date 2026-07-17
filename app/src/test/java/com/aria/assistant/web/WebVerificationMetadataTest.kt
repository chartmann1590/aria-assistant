package com.aria.assistant.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebVerificationMetadataTest {
    @Test
    fun `trace metadata round trips without page bodies`() {
        val trace = VerificationTrace(
            query = "current moon phase",
            status = VerificationStatus.VERIFIED,
            sources = listOf(WebSource("NASA", "https://nasa.gov/moon", "Moon data", extractedText = "large private page body")),
            steps = listOf(VerificationStep("Search", "Found NASA")),
            retrievedAt = 123L,
            elapsedMs = 456L
        )

        val encoded = WebVerificationMetadata.encode(trace)
        val decoded = WebVerificationMetadata.decode(encoded)!!

        assertEquals(trace.query, decoded.query)
        assertEquals(trace.status, decoded.status)
        assertEquals("NASA", decoded.sources.single().title)
        assertEquals("", decoded.sources.single().extractedText)
    }

    @Test
    fun `legacy metadata is ignored`() {
        assertNull(WebVerificationMetadata.decode("""{"type":"search_results","results":[]}"""))
    }
}

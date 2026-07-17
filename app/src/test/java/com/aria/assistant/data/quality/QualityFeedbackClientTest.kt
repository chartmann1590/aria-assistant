package com.aria.assistant.data.quality

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityFeedbackClientTest {
    private val client = QualityFeedbackClient()

    @Test
    fun `conversation text is omitted unless user opts in`() {
        val payload = client.buildPayload(
            QualityFeedback(
                category = "incorrect",
                prompt = "private prompt",
                response = "private response",
                shareContent = false,
            )
        )

        assertFalse(payload.has("prompt"))
        assertFalse(payload.has("response"))
        assertTrue(payload.getInt("responseLength") > 0)
    }

    @Test
    fun `conversation text is included after explicit opt in`() {
        val payload = client.buildPayload(
            QualityFeedback(
                category = "other",
                prompt = "shared prompt",
                response = "shared response",
                shareContent = true,
            )
        )

        assertTrue(payload.has("prompt"))
        assertTrue(payload.has("response"))
    }
}

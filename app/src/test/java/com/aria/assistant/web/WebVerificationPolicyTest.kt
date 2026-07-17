package com.aria.assistant.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebVerificationPolicyTest {
    private val policy = WebVerificationPolicy()

    @Test
    fun `all factual mode verifies informational questions`() {
        assertTrue(policy.shouldVerify("Who is the current president of France?", WebVerificationMode.ALWAYS_FACTUAL))
        assertTrue(policy.shouldVerify("Explain how solar panels work", WebVerificationMode.ALWAYS_FACTUAL))
    }

    @Test
    fun `all factual mode keeps actions and casual chat local`() {
        assertFalse(policy.shouldVerify("Set a timer for ten minutes", WebVerificationMode.ALWAYS_FACTUAL))
        assertFalse(policy.shouldVerify("Hello!", WebVerificationMode.ALWAYS_FACTUAL))
        assertFalse(policy.shouldVerify("Write a short poem about rain", WebVerificationMode.ALWAYS_FACTUAL))
    }

    @Test
    fun `current mode limits automatic searches`() {
        assertTrue(policy.shouldVerify("What is the latest Android release?", WebVerificationMode.CURRENT_ONLY))
        assertFalse(policy.shouldVerify("How do solar panels work?", WebVerificationMode.CURRENT_ONLY))
    }

    @Test
    fun `explicit mode searches only on request`() {
        assertTrue(policy.shouldVerify("Search the web for Ada Lovelace", WebVerificationMode.EXPLICIT_ONLY))
        assertFalse(policy.shouldVerify("Who was Ada Lovelace?", WebVerificationMode.EXPLICIT_ONLY))
    }
}

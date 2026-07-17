package com.aria.assistant.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiTranslationManagerTest {

    @Test
    fun `catalog exposes all ML Kit languages with English fallback`() {
        val manager = UiTranslationManager()

        assertEquals("en", manager.targetLanguage.value)
        assertTrue(manager.supportedLanguages.size >= 50)
        assertTrue(manager.supportedLanguages.map { it.tag }.containsAll(listOf("en", "es", "ar", "zh")))
    }

    @Test
    fun `unsupported language falls back to English`() {
        val manager = UiTranslationManager()

        manager.setTargetLanguage("not-a-real-language")

        assertEquals("en", manager.targetLanguage.value)
    }
}

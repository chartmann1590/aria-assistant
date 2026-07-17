package com.aria.assistant

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aria.assistant.translation.UiTranslationManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiTranslationSmokeTest {

    @Test
    fun catalog_and_english_fallback_work_on_device() = runBlocking {
        val manager = UiTranslationManager()

        val translated = manager.translate("Settings", "en")

        assertEquals("Settings", translated)
        assertTrue(manager.isLanguageDownloaded("en"))
        assertTrue(!manager.isLanguageDownloaded("not-a-language"))
        assertTrue(manager.supportedLanguages.any { it.tag == "es" })
    }

    @Test
    fun every_catalog_language_is_accepted_by_the_model_manager() = runBlocking {
        val manager = UiTranslationManager()

        manager.supportedLanguages.forEach { language ->
            // This queries ML Kit's local model registry only; it does not start a download.
            manager.isLanguageDownloaded(language.tag)
        }
    }
}

package com.aria.assistant

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aria.assistant.web.RenderedPageExtractor
import com.aria.assistant.web.VerificationStatus
import com.aria.assistant.web.WebResearchService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebResearchLiveSmokeTest {
    @Test
    fun live_public_web_research_returns_cross_checked_sources() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = WebResearchService(RenderedPageExtractor(context))

        val result = service.research("What is the latest Android version?")

        assertTrue(result.trace.sources.size >= 2)
        assertTrue(result.trace.status == VerificationStatus.VERIFIED)
        assertTrue(result.trace.sources.all { it.url.startsWith("https://") })
    }
}

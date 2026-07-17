package com.aria.assistant

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aria.assistant.web.RenderedPageExtractor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebRendererSmokeTest {
    @Test
    fun javascript_content_is_extracted_in_sandboxed_renderer() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val extractor = RenderedPageExtractor(context)
        val text = extractor.extractHtmlFixture(
            """
            <html><body><div id="result">Loading</div>
            <script>document.getElementById('result').innerText = 'Rendered web evidence';</script>
            </body></html>
            """.trimIndent()
        )

        assertTrue("Actual rendered text: <$text>", text.contains("Rendered web evidence"))
    }
}

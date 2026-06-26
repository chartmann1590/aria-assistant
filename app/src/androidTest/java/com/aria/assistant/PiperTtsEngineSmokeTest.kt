package com.aria.assistant

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aria.assistant.engine.PiperTtsEngine
import com.aria.assistant.engine.VoiceModelManager
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PiperTtsEngineSmokeTest {

    @Test
    fun engine_instantiates() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val engine = PiperTtsEngine(ctx)
        assertNotNull("PiperTtsEngine should instantiate", engine)
    }

    @Test
    fun default_voice_info_exists() {
        val amy = VoiceModelManager.voiceInfoForId("en_US-amy-medium")
        assertNotNull("Voice info should exist for en_US-amy-medium", amy)
    }
}

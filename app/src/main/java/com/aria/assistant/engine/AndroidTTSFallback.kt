package com.aria.assistant.engine

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.aria.assistant.engine.AriaLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred

@Singleton
class AndroidTTSFallback @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isReady = true
        }
    }

    suspend fun speak(text: String, speed: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isReady) {
            AriaLogger.e("AndroidTTS", "TTS not ready")
            return
        }
        AriaLogger.d("AndroidTTS", "Speaking: \"${text.take(100)}\"")
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
        val deferred = CompletableDeferred<Unit>()
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (!deferred.isCompleted) deferred.complete(Unit)
            }
            override fun onError(utteranceId: String?) {
                if (!deferred.isCompleted) deferred.complete(Unit)
            }
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (!deferred.isCompleted) deferred.complete(Unit)
            }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aria_utt")
        deferred.await()
        AriaLogger.d("AndroidTTS", "Speech done")
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

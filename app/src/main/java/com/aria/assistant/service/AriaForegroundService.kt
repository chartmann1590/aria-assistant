package com.aria.assistant.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.domain.repository.SettingsRepository
import com.aria.assistant.engine.AriaLogger
import com.aria.assistant.engine.AriaStateManager
import com.aria.assistant.engine.AriaTTS
import com.aria.assistant.engine.IntentRouter
import com.aria.assistant.engine.LlmEngine
import com.aria.assistant.engine.VoiceActivityDetector
import com.aria.assistant.engine.WakeWordDetector
import com.aria.assistant.R
import com.aria.assistant.engine.WhisperSTT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

@AndroidEntryPoint
class AriaForegroundService : Service() {

    @Inject lateinit var wakeWordDetector: WakeWordDetector
    @Inject lateinit var whisperSTT: WhisperSTT
    @Inject lateinit var voiceActivityDetector: VoiceActivityDetector
    @Inject lateinit var intentRouter: IntentRouter
    @Inject lateinit var ariaTTS: AriaTTS
    @Inject lateinit var llmEngine: LlmEngine
    @Inject lateinit var stateManager: AriaStateManager
    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isMuted = false
    private var whisperAvailable = false
    private var conversationJob: Job? = null
    @Volatile private var wakeWordEnabled = true

    override fun onCreate() {
        super.onCreate()
        AriaLogger.d("AriaForegroundService", "Service onCreate")
        startForeground(NOTIFICATION_ID, buildNotification(AriaState.INITIALIZING))
        initializeEngines()
        observeWakeWordSetting()
    }

    private fun observeWakeWordSetting() {
        serviceScope.launch {
            settingsRepository.getVoiceConfig().collect { config ->
                wakeWordEnabled = config.wakeWordEnabled
                AriaLogger.d("AriaForegroundService", "Wake word enabled changed: $wakeWordEnabled")
                if (!config.wakeWordEnabled) {
                    wakeWordDetector.stop()
                    AriaLogger.d("AriaForegroundService", "Wake word detector stopped (disabled in settings)")
                } else {
                    maybeStartWakeWord()
                }
            }
        }
    }

    private fun initializeEngines() {
        serviceScope.launch {
            try {
                AriaLogger.d("AriaForegroundService", "Initializing engines (whisper + piper)...")

                val whisperDir = File(filesDir, "models/whisper").absolutePath
                val whisperEncoder = File(whisperDir, "encoder.onnx")
                val whisperDecoder = File(whisperDir, "decoder.onnx")
                whisperAvailable = whisperEncoder.exists() && whisperDecoder.exists()
                if (whisperAvailable) {
                    AriaLogger.d("AriaForegroundService", "Whisper models found, initializing")
                    whisperSTT.initialize(whisperDir)
                } else {
                    AriaLogger.d("AriaForegroundService", "Whisper not available, will use Android STT fallback")
                }

                val piperDir = File(filesDir, "models/piper").absolutePath
                File(piperDir).mkdirs()
                ariaTTS.initialize(piperDir)

                val engineReady = llmEngine.isReady.value
                AriaLogger.d("AriaForegroundService", "Engine init complete (whisper=$whisperAvailable, engine=$engineReady)")
                updateNotification(AriaState.IDLE)
                maybeStartWakeWord()
            } catch (e: Exception) {
                AriaLogger.e("AriaForegroundService", "Engine init failed: ${e.message}", e)
                updateNotification(AriaState.IDLE)
                maybeStartWakeWord()
            }
        }
    }

    private fun getModelPath(): String? {
        val modelFile = File(filesDir, "models/gemma-4-E2B-it.litertlm")
        return if (modelFile.exists()) modelFile.absolutePath else null
    }

    private fun startWakeWordDetection() {
        wakeWordDetector.start(threshold = 0.5f) {
            serviceScope.launch { startConversation() }
        }
    }

    private fun maybeStartWakeWord() {
        if (isMuted) {
            AriaLogger.d("AriaForegroundService", "Not starting wake word — muted")
            return
        }
        if (wakeWordEnabled && stateManager.state.value == AriaState.IDLE && conversationJob == null) {
            AriaLogger.d("AriaForegroundService", "Starting wake word detection")
            startWakeWordDetection()
        }
    }

    private fun startConversation() {
        if (conversationJob?.isActive == true) return
        conversationJob = serviceScope.launch {
            try {
                onWakeWordDetected()
            } finally {
                conversationJob = null
            }
        }
    }

    private fun cancelConversation() {
        AriaLogger.d("AriaForegroundService", "Cancelling conversation")
        conversationJob?.cancel()
        conversationJob = null
        ariaTTS.stopSpeaking()
        intentRouter.stopSpeaking()
        wakeWordDetector.stop()
        stateManager.setState(AriaState.IDLE)
        updateNotification(AriaState.IDLE)
        serviceScope.launch {
            kotlinx.coroutines.delay(500)
            maybeStartWakeWord()
        }
    }

    private var conversationRound = 0

    private suspend fun onWakeWordDetected() {
        val current = stateManager.state.value
        AriaLogger.d("AriaForegroundService", "Wake word/trigger fired, muted=$isMuted, state=$current")
        if (isMuted) return
        if (current == AriaState.LISTENING || current == AriaState.PROCESSING) {
            AriaLogger.d("AriaForegroundService", "Already in conversation, ignoring trigger")
            return
        }
        if (current == AriaState.SPEAKING) {
            ariaTTS.stopSpeaking()
        }

        wakeWordDetector.stop()
        stateManager.setState(AriaState.WAKING_UP)
        updateNotification(AriaState.WAKING_UP)
        kotlinx.coroutines.delay(600)
        if (isMuted) {
            AriaLogger.d("AriaForegroundService", "Cancelling — muted before listen")
            stateManager.setState(AriaState.MUTED)
            updateNotification(AriaState.MUTED)
            return
        }
        stateManager.setState(AriaState.LISTENING)
        updateNotification(AriaState.LISTENING)
        playActivationPing()

        val transcript = recognizeWithAndroidSTT()
        if (transcript.isBlank()) {
            AriaLogger.d("AriaForegroundService", "First listen empty, resuming wake word")
            stateManager.setState(AriaState.IDLE)
            updateNotification(AriaState.IDLE)
            kotlinx.coroutines.delay(1500)
            resumeWakeWord()
            return
        }
        processAndRespond(transcript)
        // Continuous conversation
        conversationRound = 0
        listenForFollowUp()
    }

    private suspend fun processAndRespond(transcript: String) {
        if (transcript.isBlank()) return
        AriaLogger.d("AriaForegroundService", "Transcript: \"$transcript\"")
        stateManager.setState(AriaState.PROCESSING)
        updateNotification(AriaState.PROCESSING)
        intentRouter.process(transcript) { state ->
            AriaLogger.d("AriaForegroundService", "State -> $state")
            stateManager.setState(state)
            updateNotification(state)
        }
        // Wait for TTS to finish speaking before listening again
        waitForTtsSilence()
    }

    private suspend fun waitForTtsSilence() {
        var waited = 0
        while (waited < 30) {
            if (!ariaTTS.isSpeaking) {
                kotlinx.coroutines.delay(300)
                if (!ariaTTS.isSpeaking) break
            }
            kotlinx.coroutines.delay(200)
            waited++
        }
    }

    private suspend fun listenForFollowUp() {
        AriaLogger.d("AriaForegroundService", "Conversation mode start")
        // Wait for TTS + extra settling time
        kotlinx.coroutines.delay(1500)
        while (conversationRound < 3) {
            if (isMuted) {
                AriaLogger.d("AriaForegroundService", "Stopping follow-up — muted")
                break
            }
            AriaLogger.d("AriaForegroundService", "Follow-up round ${conversationRound + 1}/3")
            stateManager.setState(AriaState.LISTENING)
            updateNotification(AriaState.LISTENING)
            playActivationPing()

            val transcript = recognizeWithAndroidSTT()
            if (transcript.isBlank()) {
                conversationRound++
                kotlinx.coroutines.delay(1500)
                continue
            }
            val lower = transcript.lowercase().trim()
            if (lower in listOf("never mind", "stop", "that's all", "goodbye", "bye", "cancel", "exit", "no")) {
                AriaLogger.d("AriaForegroundService", "User ended: \"$lower\"")
                break
            }
            conversationRound = 0
            processAndRespond(transcript)
            kotlinx.coroutines.delay(1000)
        }
        AriaLogger.d("AriaForegroundService", "Conversation done")
        stateManager.setState(AriaState.IDLE)
        updateNotification(AriaState.IDLE)
        resumeWakeWord()
    }

    private fun resumeWakeWord() {
        if (isMuted) {
            AriaLogger.d("AriaForegroundService", "Not resuming wake word — muted")
            return
        }
        if (wakeWordEnabled) {
            AriaLogger.d("AriaForegroundService", "Resuming wake word detection")
            startWakeWordDetection()
        } else {
            AriaLogger.d("AriaForegroundService", "Wake word disabled, not resuming")
        }
    }

    private suspend fun recognizeWithAndroidSTT(): String = suspendCancellableCoroutine { cont ->
        AriaLogger.d("AriaForegroundService", "Creating Android SpeechRecognizer...")
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            AriaLogger.e("AriaForegroundService", "Speech recognition not available on device")
            cont.resume("")
            return@suspendCancellableCoroutine
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        if (recognizer == null) {
            AriaLogger.e("AriaForegroundService", "SpeechRecognizer.create failed")
            cont.resume("")
            return@suspendCancellableCoroutine
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                AriaLogger.d("AriaForegroundService", "STT ready")
            }
            override fun onBeginningOfSpeech() {
                AriaLogger.d("AriaForegroundService", "STT speech started")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                AriaLogger.d("AriaForegroundService", "STT speech ended")
            }
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "audio"
                    SpeechRecognizer.ERROR_CLIENT -> "client"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "no perm"
                    SpeechRecognizer.ERROR_NETWORK -> "network"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "no match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "busy"
                    SpeechRecognizer.ERROR_SERVER -> "server"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "speech timeout"
                    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "too many"
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "lang not supported"
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "lang unavailable"
                    else -> "$error"
                }
                AriaLogger.e("AriaForegroundService", "STT error=$msg")
                recognizer.destroy()
                if (!cont.isCompleted) cont.resume("")
            }
            override fun onResults(results: Bundle?) {
                val all = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = all?.firstOrNull() ?: ""
                AriaLogger.d("AriaForegroundService", "STT text='$text' (${all?.size ?: 0} alternatives)")
                recognizer.destroy()
                if (!cont.isCompleted) cont.resume(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        recognizer.setRecognitionListener(listener)
        recognizer.startListening(intent)
        AriaLogger.d("AriaForegroundService", "STT listening started...")

        cont.invokeOnCancellation {
            recognizer.destroy()
        }
    }

    private suspend fun captureSpeech(): ShortArray {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return ShortArray(0)

        val sampleRate = 16000
        val chunkSize = 1280
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(chunkSize * 2)

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) return ShortArray(0)

        audioRecord.startRecording()

        val audioFlow = flow {
            val chunk = ShortArray(chunkSize)
            while (true) {
                val read = audioRecord.read(chunk, 0, chunkSize)
                if (read > 0) {
                    emit(if (read == chunkSize) chunk else chunk.copyOf(read))
                }
            }
        }

        var result = ShortArray(0)

        try {
            voiceActivityDetector.collectSpeechSegment(audioFlow).collect { segment ->
                result = segment
            }
        } finally {
            try { audioRecord.stop() } catch (_: Exception) {}
            audioRecord.release()
        }

        return result
    }

    private suspend fun playActivationPing() {
        val toneGen = ToneGenerator(ToneGenerator.TONE_PROP_PROMPT, 50)
        try {
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK)
            kotlinx.coroutines.delay(100)
        } finally {
            toneGen.release()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MUTE -> toggleMute()
            ACTION_TRIGGER -> {
                AriaLogger.d("AriaForegroundService", "Manual trigger from UI")
                startConversation()
            }
            ACTION_CANCEL -> cancelConversation()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        AriaLogger.d("AriaForegroundService", "Service onDestroy")
        wakeWordDetector.stop()
        ariaTTS.stopSpeaking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun toggleMute() {
        isMuted = !isMuted
        if (isMuted) {
            AriaLogger.d("AriaForegroundService", "Muted - stopping wake word + conversation")
            conversationJob?.cancel()
            conversationJob = null
            wakeWordDetector.stop()
            ariaTTS.stopSpeaking()
            intentRouter.stopSpeaking()
            stateManager.setState(AriaState.MUTED)
        } else {
            AriaLogger.d("AriaForegroundService", "Unmuted - starting wake word")
            stateManager.setState(AriaState.IDLE)
            startWakeWordDetection()
        }
        updateNotification(if (isMuted) AriaState.MUTED else AriaState.IDLE)
    }

    private fun updateNotification(state: AriaState) {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: AriaState): Notification {
        val stateLabel = when (state) {
            AriaState.IDLE -> "Listening for 'Hey Aria'..."
            AriaState.LISTENING -> "Listening..."
            AriaState.PROCESSING -> "Thinking..."
            AriaState.SPEAKING -> "Speaking..."
            AriaState.MUTED -> "Muted"
            AriaState.INITIALIZING -> "Starting up..."
            AriaState.ERROR -> "Error — tap to restart"
            AriaState.DOWNLOADING -> "Downloading..."
            AriaState.WAKING_UP -> "Waking up..."
        }

        val muteLabel = if (isMuted) "Unmute" else "Mute"

        val muteIntent = PendingIntent.getService(
            this, 0,
            Intent(this, AriaForegroundService::class.java).apply { action = ACTION_MUTE },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_aria_orb)
            .setContentTitle("Aria")
            .setContentText(stateLabel)
            .addAction(R.drawable.ic_mute, muteLabel, muteIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "aria_service"
        const val ACTION_MUTE = "com.aria.assistant.MUTE"
        const val ACTION_TRIGGER = "com.aria.assistant.TRIGGER"
        const val ACTION_CANCEL = "com.aria.assistant.CANCEL"
        const val ACTION_STOP = "com.aria.assistant.STOP"
    }
}

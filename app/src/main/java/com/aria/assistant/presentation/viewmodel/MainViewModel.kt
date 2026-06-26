package com.aria.assistant.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.assistant.data.model.ConversationMessage
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.domain.repository.ConversationRepository
import com.aria.assistant.engine.AriaLogger
import com.aria.assistant.engine.AriaStateManager
import com.aria.assistant.engine.AriaTTS
import com.aria.assistant.engine.IntentRouter
import com.aria.assistant.engine.LlmEngine
import com.aria.assistant.engine.ModelDownloadManager
import com.aria.assistant.engine.VoiceModelManager
import com.aria.assistant.service.AriaServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DownloadInfo(
    val gemmaProgress: Float = 0f,
    val whisperProgress: Float = 0f,
    val voiceProgress: Float = 0f,
    val totalProgress: Float = 0f
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val downloadManager: ModelDownloadManager,
    private val llmEngine: LlmEngine,
    private val intentRouter: IntentRouter,
    private val voiceModelManager: VoiceModelManager,
    private val ariaTTS: AriaTTS,
    private val serviceController: AriaServiceController,
    private val stateManager: AriaStateManager
) : ViewModel() {

    val ariaState: StateFlow<AriaState> = stateManager.state

    private val _messages = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val messages: StateFlow<List<ConversationMessage>> = _messages.asStateFlow()

    private val _recentMessages = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val recentMessages: StateFlow<List<ConversationMessage>> = _recentMessages.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _downloadInfo = MutableStateFlow(DownloadInfo())
    val downloadInfo: StateFlow<DownloadInfo> = _downloadInfo.asStateFlow()

    private var isMuted = false

    private val modelUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    private val modelFilename = "gemma-4-E2B-it.litertlm"

    companion object {
        private const val WHISPER_ENCODER_URL = "https://huggingface.co/k2fsa/sherpa-onnx-whisper-tiny.en/resolve/main/encoder.onnx"
        private const val WHISPER_DECODER_URL = "https://huggingface.co/k2fsa/sherpa-onnx-whisper-tiny.en/resolve/main/decoder.onnx"
    }

    init {
        viewModelScope.launch {
            conversationRepository.getRecentMessages().collect { msgs ->
                _messages.value = msgs
                _recentMessages.value = msgs
            }
        }
        viewModelScope.launch {
            downloadManager.state.collect { state ->
                when (state) {
                    is ModelDownloadManager.DownloadState.Downloading -> {
                        val info = _downloadInfo.value
                        val gemmaProgress = state.bytesLoaded.toFloat() / state.totalBytes.coerceAtLeast(1)
                        _downloadInfo.value = info.copy(
                            gemmaProgress = gemmaProgress,
                            totalProgress = gemmaProgress * 0.6f
                        )
                    }
                    is ModelDownloadManager.DownloadState.Complete -> {
                        if (llmEngine.isReady.value) {
                            AriaLogger.d("MainViewModel", "Download Complete but engine already ready, skipping")
                            return@collect
                        }
                        stateManager.setState(AriaState.WAKING_UP)
                        AriaLogger.d("MainViewModel", "Model downloaded, initializing engine...")
                        llmEngine.initialize(state.modelPath)
                    }
                    is ModelDownloadManager.DownloadState.Error -> {
                        AriaLogger.e("MainViewModel", "Gemma download failed: ${state.message}")
                        stateManager.setState(AriaState.ERROR)
                    }
                    is ModelDownloadManager.DownloadState.Idle -> {}
                }
            }
        }
        viewModelScope.launch {
            llmEngine.isReady.collect { ready ->
                if (ready) {
                    AriaLogger.d("MainViewModel", "Engine ready, starting foreground service")
                    stateManager.setState(AriaState.IDLE)
                    startWhisperDownload()
                    startForegroundService()
                }
            }
        }
        viewModelScope.launch {
            intentRouter.streamingText.collect { text ->
                _streamingText.value = text
            }
        }
        startColdBoot()
    }

    private fun startColdBoot() {
        if (llmEngine.isReady.value) {
            AriaLogger.d("MainViewModel", "Engine already ready (from onboarding), skipping cold boot")
            stateManager.setState(AriaState.IDLE)
            startForegroundService()
            return
        }
        AriaLogger.d("MainViewModel", "Cold boot starting")
        stateManager.setState(AriaState.INITIALIZING)
        viewModelScope.launch {
            try {
                val cached = downloadManager.getCachedModelPath(modelFilename)
                if (cached != null) {
                    AriaLogger.d("MainViewModel", "Cached model found at $cached, initializing engine")
                    stateManager.setState(AriaState.WAKING_UP)
                    llmEngine.initialize(cached)
                } else {
                    AriaLogger.d("MainViewModel", "No cached model, starting download")
                    stateManager.setState(AriaState.DOWNLOADING)
                    downloadManager.downloadModel(modelUrl, modelFilename)
                }
            } catch (e: Exception) {
                AriaLogger.e("MainViewModel", "Cold boot failed: ${e.message}", e)
                stateManager.setState(AriaState.ERROR)
            }
        }
    }

    private fun startWhisperDownload() {
        viewModelScope.launch {
            val encoderFile = File(context.filesDir, "models/whisper/encoder.onnx")
            val decoderFile = File(context.filesDir, "models/whisper/decoder.onnx")
            if (encoderFile.exists() && decoderFile.exists()) {
                AriaLogger.d("MainViewModel", "Whisper models already cached")
                downloadDefaultVoice()
                return@launch
            }
            AriaLogger.d("MainViewModel", "Downloading whisper models...")
            val info = _downloadInfo.value
            _downloadInfo.value = info.copy(totalProgress = 0.6f)

            try {
                encoderFile.parentFile?.mkdirs()
                downloadManager.downloadFile(WHISPER_ENCODER_URL, encoderFile)
                val info2 = _downloadInfo.value
                _downloadInfo.value = info2.copy(whisperProgress = 0.5f, totalProgress = 0.7f)
                downloadManager.downloadFile(WHISPER_DECODER_URL, decoderFile)
                val info3 = _downloadInfo.value
                _downloadInfo.value = info3.copy(whisperProgress = 1f, totalProgress = 0.8f)
                AriaLogger.d("MainViewModel", "Whisper models downloaded")
            } catch (e: Exception) {
                AriaLogger.e("MainViewModel", "Whisper download failed (non-fatal): ${e.message}")
            }
            downloadDefaultVoice()
        }
    }

    private fun downloadDefaultVoice() {
        viewModelScope.launch {
            val defaultVoiceId = "en_US-amy-medium"
            if (voiceModelManager.isVoiceDownloaded(defaultVoiceId)) {
                AriaLogger.d("MainViewModel", "Default voice already cached")
                val info = _downloadInfo.value
                _downloadInfo.value = info.copy(voiceProgress = 1f, totalProgress = 1f)
                return@launch
            }
            AriaLogger.d("MainViewModel", "Downloading default voice...")
            try {
                voiceModelManager.downloadVoice(defaultVoiceId)
                val info = _downloadInfo.value
                _downloadInfo.value = info.copy(voiceProgress = 1f, totalProgress = 1f)
                AriaLogger.d("MainViewModel", "Default voice downloaded")
            } catch (e: Exception) {
                AriaLogger.e("MainViewModel", "Voice download failed (non-fatal): ${e.message}")
                val info = _downloadInfo.value
                _downloadInfo.value = info.copy(totalProgress = 1f)
            }
        }
    }

    private fun startForegroundService() {
        val hasMic = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            AriaLogger.d("MainViewModel", "Starting foreground service")
            serviceController.startService()
        } else {
            AriaLogger.d("MainViewModel", "Mic permission not granted, staying in IDLE")
        }
    }

    fun startServiceWithMic() {
        serviceController.startService()
        stateManager.setState(AriaState.IDLE)
        AriaLogger.d("MainViewModel", "Foreground service started after mic permission")
    }

    fun needsMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun sendMessage(text: String) {
        val state = stateManager.state.value
        if (state != AriaState.IDLE && state != AriaState.LISTENING && state != AriaState.MUTED) {
            AriaLogger.d("MainViewModel", "Ignoring sendMessage in state $state")
            return
        }
        viewModelScope.launch {
            intentRouter.process(text) { newState ->
                stateManager.setState(newState)
            }
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        stateManager.setState(if (isMuted) AriaState.MUTED else AriaState.IDLE)
        serviceController.muteService()
        AriaLogger.d("MainViewModel", "Mute toggled: $isMuted")
    }

    fun stopSpeaking() {
        intentRouter.stopSpeaking()
        if (stateManager.state.value == AriaState.SPEAKING) {
            stateManager.setState(AriaState.IDLE)
        }
    }

    fun setState(state: AriaState) {
        stateManager.setState(state)
    }

    fun triggerListening() {
        val current = stateManager.state.value
        when {
            current == AriaState.MUTED -> {
                toggleMute()
            }
            current == AriaState.LISTENING ||
            current == AriaState.PROCESSING ||
            current == AriaState.SPEAKING ||
            current == AriaState.WAKING_UP -> {
                AriaLogger.d("MainViewModel", "Cancelling listening")
                serviceController.cancelListening()
            }
            current == AriaState.DOWNLOADING ||
            current == AriaState.INITIALIZING -> {
                AriaLogger.d("MainViewModel", "Cannot listen — engine not ready (state=$current)")
            }
            else -> {
                viewModelScope.launch {
                    val hasMic = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasMic) {
                        AriaLogger.d("MainViewModel", "Manual trigger - starting listening")
                        stateManager.setState(AriaState.WAKING_UP)
                        serviceController.triggerListening()
                    } else {
                        serviceController.startService()
                    }
                }
            }
        }
    }
}

package com.aria.assistant.engine

import android.app.ActivityManager
import android.content.Context
import com.aria.assistant.agent.ChatMessage
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LitertLmEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : LlmEngine {

    private var engine: Engine? = null
    private var currentModelPath: String? = null
    private val initializationMutex = Mutex()
    private val inferenceMutex = Mutex()

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    override suspend fun initialize(modelPath: String): Unit = withContext(Dispatchers.IO) {
        initializationMutex.withLock {
        if (_isReady.value && currentModelPath == modelPath) {
            AriaLogger.d("LitertLmEngine", "Already initialized, skipping")
            return@withLock
        }
        AriaLogger.d("LitertLmEngine", "Initializing with model: $modelPath")
        if (!java.io.File(modelPath).exists()) {
            val err = "Model file not found: $modelPath"
            AriaLogger.e("LitertLmEngine", err)
            _lastError.value = err
            return@withContext
        }
        val memoryInfo = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memoryInfo)
        if (memoryInfo.lowMemory) {
            val err = "Not enough free memory to load the on-device model. Close other apps and try again."
            AriaLogger.e("LitertLmEngine", err)
            _lastError.value = err
            return@withContext
        }
        try {
            engine = initializeWithFallback(modelPath)
            _isReady.value = true
            currentModelPath = modelPath
            _lastError.value = null
        } catch (e: Exception) {
            val err = "All backends failed: ${e.message}"
            AriaLogger.e("LitertLmEngine", err, e)
            _lastError.value = err
        }
        }
    }

    private fun initializeWithFallback(modelPath: String): Engine {
        val attempts = listOf(
            "GPU" to { EngineConfig(modelPath, Backend.GPU()) },
            "CPU" to { EngineConfig(modelPath, Backend.CPU()) },
        )
        var lastFailure: Exception? = null
        for ((name, createConfig) in attempts) {
            AriaLogger.d("LitertLmEngine", "Trying $name backend...")
            var candidate: Engine? = null
            try {
                candidate = Engine(createConfig())
                candidate.initialize()
                AriaLogger.d("LitertLmEngine", "Engine ready ($name)")
                return candidate
            } catch (failure: Exception) {
                lastFailure = failure
                AriaLogger.d("LitertLmEngine", "$name failed: ${failure.message}")
                if (candidate?.isInitialized() == true) {
                    runCatching { candidate.close() }
                        .onFailure { AriaLogger.e("LitertLmEngine", "Could not release failed $name backend", it) }
                }
            }
        }
        throw IllegalStateException("No LiteRT backend could load the model", lastFailure)
    }

    override fun chat(messages: List<ChatMessage>): Flow<String> = flow {
        val activeEngine = engine
        if (activeEngine == null) {
            val err = _lastError.value
            if (err != null) {
                emit("Engine failed to start: $err")
            } else {
                emit("Engine not initialized yet — model may still be downloading. Please wait.")
            }
            return@flow
        }
        try {
            inferenceMutex.withLock {
                // AgentRunner supplies the complete bounded history. A fresh native conversation
                // prevents previous turns from being appended a second time by LiteRT-LM.
                activeEngine.createConversation().use { conv ->
                    val sb = StringBuilder()
                    for (msg in messages) {
                        val role = when (msg.role) {
                            ChatMessage.Role.SYSTEM -> "system"
                            ChatMessage.Role.USER -> "user"
                            ChatMessage.Role.MODEL -> "model"
                            ChatMessage.Role.TOOL -> "user"
                        }
                        val content = if (msg.role == ChatMessage.Role.TOOL) {
                            "<tool_result>${msg.content}</tool_result>"
                        } else {
                            msg.content
                        }
                        sb.append("<start_of_turn>$role\n$content<end_of_turn>\n")
                    }
                    sb.append("<start_of_turn>model\n")

                    val resultFlow = conv.sendMessageAsync(sb.toString())
                    resultFlow.collect { token -> emit(token.toString()) }
                }
            }
        } catch (e: Exception) {
            AriaLogger.e("LitertLmEngine", "Inference failed: ${e.message}", e)
            emit("I'm sorry, I encountered an error processing your request.")
        }
    }.flowOn(Dispatchers.IO)
}

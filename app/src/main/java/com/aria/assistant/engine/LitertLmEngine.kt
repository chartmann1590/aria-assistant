package com.aria.assistant.engine

import android.content.Context
import com.aria.assistant.agent.ChatMessage
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
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
    private var conversation: Conversation? = null
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
        try {
            // Try NPU first (Tensor G3 Edge TPU), then GPU (OpenCL), then CPU
            try {
                AriaLogger.d("LitertLmEngine", "Trying NPU backend...")
                val npuConfig = EngineConfig(modelPath, Backend.NPU())
                val eng = Engine(npuConfig)
                eng.initialize()
                conversation = eng.createConversation()
                engine = eng
                AriaLogger.d("LitertLmEngine", "Engine ready (NPU)")
            } catch (e1: Exception) {
                AriaLogger.d("LitertLmEngine", "NPU failed: ${e1.message}")
                try {
                    AriaLogger.d("LitertLmEngine", "Trying GPU backend...")
                    val gpuConfig = EngineConfig(modelPath, Backend.GPU())
                    val eng = Engine(gpuConfig)
                    eng.initialize()
                    conversation = eng.createConversation()
                    engine = eng
                    AriaLogger.d("LitertLmEngine", "Engine ready (GPU)")
                } catch (e2: Exception) {
                    AriaLogger.d("LitertLmEngine", "GPU failed: ${e2.message}")
                    AriaLogger.d("LitertLmEngine", "Trying CPU backend...")
                    val cpuConfig = EngineConfig(modelPath, Backend.CPU())
                    val eng = Engine(cpuConfig)
                    eng.initialize()
                    conversation = eng.createConversation()
                    engine = eng
                    AriaLogger.d("LitertLmEngine", "Engine ready (CPU)")
                }
            }
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
                val conv = activeEngine.createConversation()
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
        } catch (e: Exception) {
            AriaLogger.e("LitertLmEngine", "Inference failed: ${e.message}", e)
            emit("I'm sorry, I encountered an error processing your request.")
        }
    }.flowOn(Dispatchers.IO)
}

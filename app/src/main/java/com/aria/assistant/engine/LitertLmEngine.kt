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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LitertLmEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : LlmEngine {

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val systemPrompt = buildString {
        append("You are Aria, a capable personal voice assistant running entirely on this device.\n\n")
        append("PERSONALITY: Warm, direct, and helpful. Respond like a knowledgeable friend — natural, not robotic. Match the user's tone.\n\n")
        append("CAPABILITIES: You can search the web in real-time for current information, set timers/alarms, send messages, make calls, and control device settings. You have access to live internet search results.\n\n")
        append("RULES:\n")
        append("- Always be honest about what you know vs what you need to look up.\n")
        append("- For factual/current questions (news, weather, sports, definitions, prices, events, etc.), trigger a web_search.\n")
        append("- For timers, alarms, calls, SMS, or settings changes, trigger the matching action.\n")
        append("- For casual conversation (greetings, jokes, opinions, thanks), respond naturally without JSON.\n")
        append("- Keep responses concise (1-3 sentences). Only elaborate when asked.\n")
        append("- Never mention being an AI, model, or training data. You are simply Aria.\n")
        append("- Never hallucinate facts. If the search results don't contain the answer, say so honestly.\n")
        append("- When the user asks follow-up questions, use the conversation context to understand what they're referring to.\n\n")
        append("ACTIONS — output a JSON block on its own line, then your spoken response:\n")
        append("{\"action\":\"web_search\",\"params\":{\"query\":\"your search query\"}}\n")
        append("{\"action\":\"set_timer\",\"params\":{\"duration_seconds\":600}}\n")
        append("{\"action\":\"set_alarm\",\"params\":{\"hour\":7,\"minute\":30,\"label\":\"wake up\"}}\n")
        append("{\"action\":\"make_call\",\"params\":{\"contact\":\"Mom\"}}\n")
        append("{\"action\":\"send_sms\",\"params\":{\"contact\":\"Mom\",\"message\":\"On my way\"}}\n")
            append("{\"action\":\"adjust_setting\",\"params\":{\"setting\":\"brightness|volume|wifi|bluetooth|dnd|airplane_mode|battery_saver\",\"value\":\"50%\"}}\n")
            append("{\"action\":\"create_calendar_event\",\"params\":{\"title\":\"Dentist\",\"start_ms\":1700000000000,\"end_ms\":1700003600000,\"location\":\"Clinic\"}}\n")
            append("{\"action\":\"list_calendar_events\",\"params\":{\"from_ms\":1700000000000,\"to_ms\":1700086400000}}\n")
            append("{\"action\":\"set_reminder\",\"params\":{\"label\":\"Buy groceries\",\"minutes_before\":15}}\n")
            append("{\"action\":\"read_notifications\",\"params\":{\"filter\":\"messages\"}}\n")
            append("{\"action\":\"reply_notification\",\"params\":{\"notification_key\":\"key\",\"reply_text\":\"On my way\"}}\n")
            append("{\"action\":\"get_location\",\"params\":{}}\n")
            append("{\"action\":\"navigate_to\",\"params\":{\"place\":\"nearest coffee shop\"}}\n")
            append("{\"action\":\"take_photo\",\"params\":{\"label\":\"document\"}}\n")
            append("{\"action\":\"get_latest_photo\",\"params\":{\"count\":3}}\n")
            append("{\"action\":\"media_control\",\"params\":{\"action\":\"play|pause|next|prev\"}}\n")
            append("{\"action\":\"launch_app\",\"params\":{\"app\":\"Spotify\"}}\n")
            append("{\"action\":\"list_apps\",\"params\":{}}\n")
            append("{\"action\":\"read_screen\",\"params\":{}}\n")
            append("{\"action\":\"click_on\",\"params\":{\"label\":\"Send\"}}\n")
            append("{\"action\":\"scroll\",\"params\":{\"direction\":\"down\"}}\n")
            append("{\"action\":\"get_battery\",\"params\":{}}\n")
            append("{\"action\":\"get_time\",\"params\":{}}\n\n")
            append("For web_search, formulate the query as if typing into a search engine — include location, year, or specifics to get the best results.\n")
            append("Device context (battery, time, Wi-Fi) is automatically provided to you before each user message.")
    }

    override suspend fun initialize(modelPath: String): Unit = withContext(Dispatchers.IO) {
        if (_isReady.value) {
            AriaLogger.d("LitertLmEngine", "Already initialized, skipping")
            return@withContext
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
            _lastError.value = null
        } catch (e: Exception) {
            val err = "All backends failed: ${e.message}"
            AriaLogger.e("LitertLmEngine", err, e)
            _lastError.value = err
        }
    }

    override fun generateResponse(userMessage: String, history: List<Pair<String, String>>): Flow<String> {
        val msgs = mutableListOf(ChatMessage(ChatMessage.Role.SYSTEM, systemPrompt))
        for ((u, a) in history.takeLast(4)) {
            msgs.add(ChatMessage(ChatMessage.Role.USER, u))
            msgs.add(ChatMessage(ChatMessage.Role.MODEL, a))
        }
        msgs.add(ChatMessage(ChatMessage.Role.USER, userMessage))
        return chat(msgs)
    }

    override fun chat(messages: List<ChatMessage>): Flow<String> = flow {
        val conv = conversation
        if (conv == null) {
            val err = _lastError.value
            if (err != null) {
                emit("Engine failed to start: $err")
            } else {
                emit("Engine not initialized yet — model may still be downloading. Please wait.")
            }
            return@flow
        }
        try {
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
        } catch (e: Exception) {
            AriaLogger.e("LitertLmEngine", "Inference failed: ${e.message}", e)
            emit("I'm sorry, I encountered an error processing your request.")
        }
    }.flowOn(Dispatchers.IO)
}

package com.aria.assistant.agent

import com.aria.assistant.billing.FeatureGate
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.domain.repository.ConversationRepository
import com.aria.assistant.domain.repository.SettingsRepository
import com.aria.assistant.engine.AriaLogger
import com.aria.assistant.engine.AriaTTS
import com.aria.assistant.engine.LlmEngine
import com.aria.assistant.permission.PhoneCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import org.json.JSONObject
import com.aria.assistant.web.VerificationStatus
import com.aria.assistant.web.VerificationStep
import com.aria.assistant.web.VerificationTrace
import com.aria.assistant.web.WebResearchResult
import com.aria.assistant.web.WebResearchService
import com.aria.assistant.web.WebVerificationMetadata
import com.aria.assistant.web.WebVerificationPolicy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRunner @Inject constructor(
    private val llmEngine: LlmEngine,
    private val tools: @JvmSuppressWildcards Set<Tool>,
    private val deviceContextProvider: DeviceContextProvider,
    private val promptBuilder: PromptBuilder,
    private val actionParser: ActionParser,
    private val ariaTTS: AriaTTS,
    private val featureGate: FeatureGate,
    private val conversationRepo: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val webVerificationPolicy: WebVerificationPolicy,
    private val webResearchService: WebResearchService
) {
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _verificationActivity = MutableStateFlow<String?>(null)
    val verificationActivity: StateFlow<String?> = _verificationActivity.asStateFlow()

    private val _permissionRequest = MutableSharedFlow<PhoneCapability>(replay = 1, extraBufferCapacity = 1)
    val permissionRequest: SharedFlow<PhoneCapability> = _permissionRequest.asSharedFlow()

    private var currentSessionId: String = "default"

    fun setSessionId(id: String) {
        currentSessionId = id
    }

    suspend fun run(transcript: String, onStateChange: (AriaState) -> Unit) {
        conversationRepo.saveMessage("user", transcript, currentSessionId)

        val deviceContext = deviceContextProvider.snapshot()
        val systemMsg = ChatMessage(ChatMessage.Role.SYSTEM, promptBuilder.buildSystemPrompt(tools, deviceContext))

        val history = buildHistory()
        var webResearch: WebResearchResult? = null
        val verificationMode = settingsRepository.getVoiceConfig().first().webVerificationMode
        if (webVerificationPolicy.shouldVerify(transcript, verificationMode)) {
            _verificationActivity.value = "Searching the web…"
            webResearch = runCatching { webResearchService.research(transcript) }.getOrElse { error ->
                AriaLogger.e("AgentRunner", "Web verification failed: ${error.message}", error)
                WebResearchResult(
                    VerificationTrace(
                        query = transcript,
                        status = VerificationStatus.UNAVAILABLE,
                        sources = emptyList(),
                        steps = listOf(VerificationStep("Search", "Web verification failed safely")),
                        retrievedAt = System.currentTimeMillis(),
                        elapsedMs = 0L,
                        warning = "Web verification could not be completed. The answer may rely on local model knowledge."
                    )
                )
            }
            _verificationActivity.value = "Cross-checking ${webResearch.trace.sources.size} sources…"
        }
        val messages = mutableListOf<ChatMessage>().apply {
            add(systemMsg)
            addAll(history)
            add(ChatMessage(ChatMessage.Role.USER, "[${deviceContext.toPromptSection()}] $transcript"))
            webResearch?.let {
                add(ChatMessage(ChatMessage.Role.TOOL, it.toPromptEvidence()))
            }
        }

        var finalText: String? = null

        for (iteration in 1..MAX_ITERATIONS) {
            onStateChange(AriaState.PROCESSING)
            _streamingText.value = ""

            val responseText = collectFullResponse(messages)
            val action = actionParser.extractAction(responseText)

            if (action == null) {
                val say = actionParser.extractSay(responseText)
                finalText = say?.ifBlank { null } ?: "(No response)"
                break
            }

            val toolName = action.optString("tool", "")
            val tool = tools.find { it.name == toolName }
            if (tool == null) {
                finalText = "I don't have a tool called '$toolName'."
                break
            }

            if (!featureGate.isAllowed(tool.name)) {
                finalText = "That requires a Premium subscription."
                break
            }

            val params = action.optJSONObject("params") ?: JSONObject()
            AriaLogger.d("AgentRunner", "Iteration $iteration: ${tool.name}($params)")

            val toolResult = tool.execute(params)
            AriaLogger.d("AgentRunner", "Result: ${toolResult::class.simpleName}")

            when (toolResult) {
                is ToolResult.Success -> {
                    toolResult.webResearch?.let { webResearch = it }
                    messages.add(ChatMessage(ChatMessage.Role.MODEL, responseText))
                    messages.add(ChatMessage(ChatMessage.Role.TOOL, "Success: ${toolResult.payload}"))
                }
                is ToolResult.Failure -> {
                    messages.add(ChatMessage(ChatMessage.Role.MODEL, responseText))
                    messages.add(ChatMessage(ChatMessage.Role.TOOL, "Error: ${toolResult.reason}"))
                }
                is ToolResult.NeedsPermission -> {
                    _permissionRequest.tryEmit(toolResult.capability)
                    _verificationActivity.value = null
                    speak("I need permission to ${toolResult.capability.rationale}. Please grant it in settings.", onStateChange)
                    return
                }
                is ToolResult.NeedsClarification -> {
                    _verificationActivity.value = null
                    speak(toolResult.question, onStateChange)
                    return
                }
                is ToolResult.Say -> {
                    finalText = toolResult.text
                    break
                }
            }

            if (iteration == MAX_ITERATIONS) {
                finalText = "I've done what I can with that request."
                AriaLogger.d("AgentRunner", "Hit max iteration cap")
            }
        }

        var output = finalText ?: "Done!"
        if (webResearch?.trace?.status == VerificationStatus.UNAVAILABLE &&
            !output.startsWith("Couldn’t verify", ignoreCase = true)
        ) {
            output = "Couldn’t verify this on the web right now. $output"
        }
        _streamingText.value = cleanForDisplay(output)
        val metadata = webResearch?.let { WebVerificationMetadata.encode(it.trace) }
        _verificationActivity.value = null
        saveAndSpeak(output, metadata, onStateChange)
    }

    private suspend fun collectFullResponse(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        llmEngine.chat(messages).collect { token ->
            sb.append(token)
        }
        return sb.toString()
    }

    private suspend fun buildHistory(): List<ChatMessage> {
        return try {
            val recent = conversationRepo.getRecentMessages().first()
            val msgs = recent.take(10).reversed()
            val result = mutableListOf<ChatMessage>()
            var i = 0
            while (i < msgs.size - 1) {
                if (msgs[i].role == "user" && msgs[i + 1].role == "aria") {
                    result.add(ChatMessage(ChatMessage.Role.USER, msgs[i].content))
                    result.add(ChatMessage(ChatMessage.Role.MODEL, msgs[i + 1].content))
                    i += 2
                } else {
                    i++
                }
            }
            result.takeLast(4)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun cleanForDisplay(text: String): String {
        return text
            .replace(Regex("</?action>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?say>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?tool_result>", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private suspend fun speak(text: String, onStateChange: (AriaState) -> Unit) {
        onStateChange(AriaState.SPEAKING)
        ariaTTS.speak(text)
        onStateChange(AriaState.IDLE)
    }

    private suspend fun saveAndSpeak(text: String, metadata: String?, onStateChange: (AriaState) -> Unit) {
        conversationRepo.saveMessage("aria", text, currentSessionId, metadata)
        speak(text.replace(Regex("\\s*\\[\\d+]"), "").replace(Regex("\\s{2,}"), " ").trim(), onStateChange)
    }

    fun stopSpeaking() {
        ariaTTS.stopSpeaking()
    }

    companion object {
        private const val MAX_ITERATIONS = 4
    }
}

package com.aria.assistant.agent

import com.aria.assistant.billing.FeatureGate
import com.aria.assistant.data.review.ReviewSignal
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.domain.repository.ConversationRepository
import com.aria.assistant.domain.repository.MemoryRepository
import com.aria.assistant.domain.repository.SettingsRepository
import com.aria.assistant.engine.AriaLogger
import com.aria.assistant.engine.AriaTTS
import com.aria.assistant.engine.LlmEngine
import com.aria.assistant.permission.PhoneCapability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

/** Strips protocol tags for display/spoken-ready text. File-level so nested helper classes can use it. */
private fun cleanTagsForDisplay(text: String): String {
    return text
        .replace(Regex("</?action>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?say>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?tool_result>", RegexOption.IGNORE_CASE), "")
}

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
    private val webResearchService: WebResearchService,
    private val reviewSignal: ReviewSignal,
    private val memoryRepository: MemoryRepository? = null,
) {
    /** Scope for the background sentence-speech drain loop. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        val memorySection = memoryRepository?.let { repo ->
            runCatching { repo.observeAll().first() }
                .getOrNull()
                ?.take(30)
                ?.joinToString("\n") { "- ${it.content}" }
                ?.takeIf { it.isNotBlank() }
        }
        val systemMsg = ChatMessage(
            ChatMessage.Role.SYSTEM,
            promptBuilder.buildSystemPrompt(tools, deviceContext, memorySection)
        )

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

        // Sentence-level streaming: complete sentences are spoken while the
        // model is still generating, cutting time-to-first-word from the full
        // generation time to roughly one sentence.
        val speechQueue = SpeechQueue(scope, ariaTTS)
        var streamSession = StreamingSession()

        for (iteration in 1..MAX_ITERATIONS) {
            onStateChange(AriaState.PROCESSING)
            _streamingText.value = ""

            val responseText = collectFullResponse(messages) { _ ->
                _streamingText.value = streamSession.displayText
                speechQueue.feed(streamSession.spokenReadyDelta())
            }
            speechQueue.feed(streamSession.spokenReadyDelta(final = true))

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

            // Finish any streamed sentence before the tool acts, then start a
            // fresh streaming session for the post-tool turn.
            speechQueue.drain()
            speechQueue.reset()
            streamSession = StreamingSession()

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
                    speechQueue.shutdown()
                    speak("I need permission to ${toolResult.capability.rationale}. Please grant it in settings.", onStateChange)
                    return
                }
                is ToolResult.NeedsClarification -> {
                    _verificationActivity.value = null
                    speechQueue.shutdown()
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

        // Speak only what streaming didn't already speak. If the final output
        // diverges from the streamed prefix (e.g. the "Couldn't verify" prefix
        // was added), restart speech cleanly to avoid stutter.
        val spokenPrefix = streamSession.spokenSoFar().trim()
        conversationRepo.saveMessage("aria", output, currentSessionId, metadata)
        reviewSignal.recordSuccessfulResponse()
        speechQueue.shutdown()
        val toSpeak = when {
            spokenPrefix.isBlank() -> output
            output.startsWith(spokenPrefix) && output.length > spokenPrefix.length ->
                output.substring(spokenPrefix.length)
            else -> {
                ariaTTS.stopSpeaking()
                output
            }
        }
        if (toSpeak.isNotBlank()) {
            speak(spokenText(toSpeak), onStateChange)
        } else {
            onStateChange(AriaState.IDLE)
        }
    }

    private suspend fun collectFullResponse(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit
    ): String {
        val sb = StringBuilder()
        llmEngine.chat(messages).collect { token ->
            sb.append(token)
            onToken(token)
        }
        return sb.toString()
    }

    /**
     * Accumulates streaming tokens and extracts the "spoken-ready" region:
     * complete sentences inside an active <say> block (or plain text when no
     * protocol tags are present). Text inside an <action> block is never spoken.
     */
    private class StreamingSession {
        private val raw = StringBuilder()
        private var spokenUpTo = 0 // absolute index into raw, up to last sentence boundary

        /** Display text with protocol tags stripped, for the live UI. */
        val displayText: String
            get() = cleanTagsForDisplay(raw.toString())

        fun append(token: String) {
            raw.append(token)
        }

        /**
         * Returns the next complete-sentence chunk to speak, or "" if none is
         * ready yet. With [final], returns whatever remains even if it does not
         * end at a sentence boundary (end of this generation turn).
         */
        fun spokenReadyDelta(final: Boolean = false): String {
            val text = raw.toString()
            val region = activeSpokenRegion(text) ?: return ""
            if (region.start < spokenUpTo) {
                // Region moved backwards (new turn after tool result): resync.
                spokenUpTo = region.start
            }
            val pending = text.substring(spokenUpTo.coerceAtLeast(region.start), region.endInclusive + 1)
            val boundary = sentenceBoundary(pending)
            if (boundary == null) {
                return if (final) pending.trim() else ""
            }
            val chunk = pending.take(boundary)
            spokenUpTo += boundary
            return chunk.trim()
        }

        /** Everything already handed to TTS for this turn. */
        fun spokenSoFar(): String {
            val text = raw.toString()
            val region = activeSpokenRegion(text) ?: return ""
            val end = spokenUpTo.coerceIn(region.start, region.endInclusive + 1)
            return text.substring(region.start, end)
        }

        /**
         * The speakable span of [text]: inside <say>…</say> if present (an
         * unclosed <say> streams its content), otherwise the whole text when no
         * <action> block has appeared. Returns null while an action is being
         * generated (never speak tool JSON).
         */
        private fun activeSpokenRegion(text: String): TextRange? {
            val sayOpen = text.indexOf("<say>", ignoreCase = true)
            if (sayOpen >= 0) {
                val contentStart = sayOpen + "<say>".length
                val sayClose = text.indexOf("</say>", contentStart, ignoreCase = true)
                val contentEnd = if (sayClose >= 0) sayClose - 1 else text.length - 1
                if (contentEnd < contentStart) return null
                return TextRange(contentStart, contentEnd)
            }
            val actionOpen = text.indexOf("<action>", ignoreCase = true)
            if (actionOpen >= 0) return null
            return TextRange(0, text.length - 1)
        }

        /** Index just past the last complete sentence in [text], or null. */
        private fun sentenceBoundary(text: String): Int? {
            val trimmedEnd = text.trimEnd().length
            if (trimmedEnd < MIN_SPEAK_SENTENCE_CHARS) return null
            for (i in trimmedEnd - 1 downTo MIN_SPEAK_SENTENCE_CHARS - 1) {
                val c = text[i]
                if (c == '.' || c == '!' || c == '?') {
                    val next = text.getOrNull(i + 1)
                    if (next == null || next == ' ' || next == '\n') return i + 1
                }
            }
            return null
        }

        private data class TextRange(val start: Int, val endInclusive: Int)

        private companion object {
            /** Don't speak tiny fragments — wait for a real sentence. */
            const val MIN_SPEAK_SENTENCE_CHARS = 25
        }
    }

    /** Serializes sentence-level speech so sentences play in order. */
    private class SpeechQueue(
        private val scope: CoroutineScope,
        private val ariaTTS: AriaTTS
    ) {
        private val pending = ArrayDeque<String>()
        private var job: Job? = null
        private var stopped = false

        fun feed(text: String) {
            if (stopped || text.isBlank()) return
            synchronized(pending) { pending.addLast(text.trim()) }
            ensureDraining()
        }

        /** Waits until everything queued has been spoken. */
        suspend fun drain() {
            job?.join()
        }

        fun reset() {
            synchronized(pending) { pending.clear() }
        }

        fun shutdown() {
            stopped = true
            synchronized(pending) { pending.clear() }
            job = null
        }

        private fun ensureDraining() {
            if (job?.isActive == true) return
            job = scope.launch {
                while (true) {
                    val next = synchronized(pending) { pending.removeFirstOrNull() } ?: break
                    ariaTTS.speak(next)
                }
            }
        }
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

    private fun cleanForDisplay(text: String): String = cleanTagsForDisplay(text).trim()

    private fun spokenText(text: String): String {
        return text.replace(Regex("\\s*\\[\\d+]"), "").replace(Regex("\\s{2,}"), " ").trim()
    }

    private suspend fun speak(text: String, onStateChange: (AriaState) -> Unit) {
        if (text.isBlank()) {
            onStateChange(AriaState.IDLE)
            return
        }
        onStateChange(AriaState.SPEAKING)
        ariaTTS.speak(text)
        onStateChange(AriaState.IDLE)
    }

    fun stopSpeaking() {
        ariaTTS.stopSpeaking()
    }

    companion object {
        private const val MAX_ITERATIONS = 4
    }
}

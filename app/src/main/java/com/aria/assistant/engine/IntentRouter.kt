package com.aria.assistant.engine

import com.aria.assistant.agent.AgentRunner
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.permission.PhoneCapability
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin facade over [AgentRunner].
 *
 * Historically this class also "routed" LLM output to a parallel sealed
 * [com.aria.assistant.domain.model.AriaIntent] hierarchy, but every tool is
 * now executed by the agent loop and that layer had no production callers —
 * only a test file that tested it against itself. Intent resolution is the
 * LLM+<action> loop; this class stays as the seam the service and view models
 * talk to.
 */
@Singleton
class IntentRouter @Inject constructor(
    private val agentRunner: AgentRunner
) {
    val streamingText: StateFlow<String> = agentRunner.streamingText
    val verificationActivity: StateFlow<String?> = agentRunner.verificationActivity
    val permissionRequest: SharedFlow<PhoneCapability> = agentRunner.permissionRequest

    fun setSessionId(id: String) {
        agentRunner.setSessionId(id)
    }

    suspend fun process(transcript: String, onStateChange: (AriaState) -> Unit) {
        agentRunner.run(transcript, onStateChange)
    }

    fun stopSpeaking() {
        agentRunner.stopSpeaking()
    }
}

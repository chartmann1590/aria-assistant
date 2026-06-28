package com.aria.assistant.engine

import com.aria.assistant.agent.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LlmEngine {
    val isReady: StateFlow<Boolean>
    suspend fun initialize(modelPath: String)
    fun chat(messages: List<ChatMessage>): Flow<String>
}

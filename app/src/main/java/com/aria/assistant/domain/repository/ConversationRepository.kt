package com.aria.assistant.domain.repository

import com.aria.assistant.data.model.ConversationMessage
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getRecentMessages(): Flow<List<ConversationMessage>>
    suspend fun saveMessage(role: String, content: String, sessionId: String, metadata: String? = null)
    suspend fun deleteSession(sessionId: String)
}

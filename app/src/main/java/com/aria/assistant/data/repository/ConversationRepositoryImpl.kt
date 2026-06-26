package com.aria.assistant.data.repository

import com.aria.assistant.data.model.ConversationMessage
import com.aria.assistant.data.source.ConversationDao
import com.aria.assistant.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val dao: ConversationDao
) : ConversationRepository {

    override fun getRecentMessages(): Flow<List<ConversationMessage>> = dao.getRecent()

    override suspend fun saveMessage(role: String, content: String, sessionId: String, metadata: String?) {
        dao.insert(ConversationMessage(role = role, content = content, sessionId = sessionId, metadata = metadata))
    }

    override suspend fun deleteSession(sessionId: String) = dao.deleteSession(sessionId)
}

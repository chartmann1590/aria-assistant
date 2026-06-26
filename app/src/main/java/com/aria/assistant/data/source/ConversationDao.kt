package com.aria.assistant.data.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aria.assistant.data.model.ConversationMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation_messages ORDER BY timestamp DESC LIMIT 50")
    fun getRecent(): Flow<List<ConversationMessage>>

    @Insert
    suspend fun insert(message: ConversationMessage)

    @Query("DELETE FROM conversation_messages WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}

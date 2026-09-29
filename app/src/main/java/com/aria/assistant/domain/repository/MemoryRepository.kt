package com.aria.assistant.domain.repository

import com.aria.assistant.data.model.Memory
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun observeAll(): Flow<List<Memory>>
    suspend fun add(content: String): Result<Memory>
    suspend fun update(id: Long, content: String): Boolean
    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun search(query: String): List<Memory>
    suspend fun recentForPrompt(limit: Int = 20): List<Memory>
    suspend fun count(): Int
}

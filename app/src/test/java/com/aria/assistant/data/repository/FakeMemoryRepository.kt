package com.aria.assistant.data.repository

import com.aria.assistant.data.model.Memory
import com.aria.assistant.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory fake for agent tests; keeps the memory tools testable without Room. */
class FakeMemoryRepository : MemoryRepository {

    val memories = MutableStateFlow<List<Memory>>(emptyList())
    var nextId = 1L

    override fun observeAll(): Flow<List<Memory>> = memories

    override suspend fun add(content: String): Result<Memory> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Memory is empty"))
        if (trimmed.length > MemoryRepositoryImpl.MAX_LENGTH) {
            return Result.failure(IllegalArgumentException("Memory too long"))
        }
        val memory = Memory(id = nextId++, content = trimmed)
        memories.value = memories.value + memory
        return Result.success(memory)
    }

    override suspend fun update(id: Long, content: String): Boolean {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return false
        val updated = memories.value.map {
            if (it.id == id) it.copy(content = trimmed, updatedAt = System.currentTimeMillis()) else it
        }
        val changed = updated != memories.value
        memories.value = updated
        return changed
    }

    override suspend fun delete(id: Long) {
        memories.value = memories.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        memories.value = emptyList()
    }

    override suspend fun search(query: String): List<Memory> =
        memories.value.filter { it.content.contains(query, ignoreCase = true) }

    override suspend fun recentForPrompt(limit: Int): List<Memory> =
        memories.value.takeLast(limit)

    override suspend fun count(): Int = memories.value.size
}

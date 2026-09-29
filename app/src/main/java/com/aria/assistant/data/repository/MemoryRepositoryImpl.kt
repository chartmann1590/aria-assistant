package com.aria.assistant.data.repository

import com.aria.assistant.data.model.Memory
import com.aria.assistant.data.source.MemoryDao
import com.aria.assistant.domain.repository.MemoryRepository
import com.aria.assistant.engine.AriaLogger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val memoryDao: MemoryDao
) : MemoryRepository {

    override fun observeAll(): Flow<List<Memory>> = memoryDao.observeRecent()

    override suspend fun add(content: String): Result<Memory> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Memory is empty"))
        if (trimmed.length > MAX_LENGTH) {
            return Result.failure(IllegalArgumentException("Memory too long (max $MAX_LENGTH chars)"))
        }
        // Simple dedupe: identical content updates the existing memory's timestamp
        // instead of piling up duplicates.
        val existing = memoryDao.search(trimmed).firstOrNull { it.content.equals(trimmed, ignoreCase = true) }
        val memory = if (existing != null) {
            existing.copy(content = trimmed, updatedAt = System.currentTimeMillis())
        } else {
            Memory(content = trimmed)
        }
        val id = memoryDao.insert(memory)
        val saved = if (id > 0) memory.copy(id = id) else memory
        AriaLogger.d("MemoryRepository", "Saved memory #${saved.id}")
        return Result.success(saved)
    }

    override suspend fun update(id: Long, content: String): Boolean {
        val trimmed = content.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) return false
        val all = memoryDao.getRecent(200)
        val existing = all.firstOrNull { it.id == id } ?: return false
        memoryDao.update(existing.copy(content = trimmed, updatedAt = System.currentTimeMillis()))
        return true
    }

    override suspend fun delete(id: Long) = memoryDao.deleteById(id)

    override suspend fun deleteAll() {
        memoryDao.deleteAll()
        AriaLogger.d("MemoryRepository", "Cleared all memories")
    }

    override suspend fun search(query: String): List<Memory> =
        memoryDao.search(query.trim())

    override suspend fun recentForPrompt(limit: Int): List<Memory> =
        memoryDao.getRecent(limit)

    override suspend fun count(): Int = memoryDao.count()

    companion object {
        const val MAX_LENGTH = 500
    }
}

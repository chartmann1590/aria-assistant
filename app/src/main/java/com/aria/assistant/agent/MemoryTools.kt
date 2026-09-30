package com.aria.assistant.agent

import com.aria.assistant.data.repository.MemoryRepositoryImpl
import com.aria.assistant.domain.repository.MemoryRepository
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device memory tools. Facts live in a Room table on this device only and
 * are injected into the system prompt context on every turn, so "remember
 * that Mom's number is 555-0100" persists across sessions without any server.
 */
@Singleton
class SaveMemoryTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : Tool {
    override val name = "save_memory"
    override val description =
        "Remember a durable fact about the user for future conversations (e.g. a preference, a name, a phone number)"
    override val paramSchema = """{"fact": "Mom's phone number is 555-0100"}"""
    override val requiresPremium = false

    override suspend fun execute(params: JSONObject): ToolResult {
        val fact = params.optString("fact", "").trim()
        if (fact.isBlank()) {
            return ToolResult.Failure("Tell me the fact to remember, e.g. {\"fact\": \"...\"}")
        }
        if (fact.length > MemoryRepositoryImpl.MAX_LENGTH) {
            return ToolResult.Failure("That fact is too long to remember.")
        }
        return memoryRepository.add(fact).fold(
            onSuccess = { ToolResult.Say("Got it — I'll remember that.") },
            onFailure = { ToolResult.Failure(it.message ?: "I couldn't save that memory.") }
        )
    }
}

@Singleton
class RecallMemoryTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : Tool {
    override val name = "recall_memory"
    override val description =
        "Search previously remembered facts about the user; leave query empty to list the most recent ones"
    override val paramSchema = """{"query": "Mom"}"""
    override val requiresPremium = false

    override suspend fun execute(params: JSONObject): ToolResult {
        val query = params.optString("query", "").trim()
        val memories = if (query.isBlank()) {
            memoryRepository.recentForPrompt(limit = 10)
        } else {
            memoryRepository.search(query)
        }
        if (memories.isEmpty()) {
            return ToolResult.Success(
                if (query.isBlank()) "No memories stored yet."
                else "No memories match \"$query\"."
            )
        }
        // Include each memory's ID so forget_memory can target one fact.
        val payload = memories.joinToString("\n") { "- [id=${it.id}] ${it.content}" }
        return ToolResult.Success("Remembered facts:\n$payload")
    }
}

@Singleton
class ForgetMemoryTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : Tool {
    override val name = "forget_memory"
    override val description =
        "Delete a remembered fact. Pass id to delete one memory (from recall_memory results), or all=true to forget everything"
    override val paramSchema = """{"id": 3, "all": false}"""
    override val requiresPremium = false

    override suspend fun execute(params: JSONObject): ToolResult {
        val forgetAll = params.optBoolean("all", false)
        if (forgetAll) {
            memoryRepository.deleteAll()
            return ToolResult.Say("Done — I've forgotten everything.")
        }
        val id = if (params.has("id")) params.optLong("id", -1L) else -1L
        if (id <= 0) {
            return ToolResult.Failure(
                "Tell me which memory to forget: {\"id\": 3} (from recall_memory) or {\"all\": true}."
            )
        }
        memoryRepository.delete(id)
        return ToolResult.Say("Forgotten.")
    }
}

package com.aria.assistant.agent

import com.aria.assistant.data.repository.FakeMemoryRepository
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryToolsTest {

    private val repo = FakeMemoryRepository()

    @Test
    fun `save_memory stores fact and speaks confirmation`() = runTest {
        val tool = SaveMemoryTool(repo)
        val result = tool.execute(JSONObject("""{"fact": "Mom's number is 555-0100"}"""))
        assertTrue(result is ToolResult.Say)
        assertEquals(1, repo.count())
        assertEquals("Mom's number is 555-0100", repo.recentForPrompt().first().content)
    }

    @Test
    fun `save_memory rejects blank fact`() = runTest {
        val result = SaveMemoryTool(repo).execute(JSONObject("""{"fact": "  "}"""))
        assertTrue(result is ToolResult.Failure)
        assertEquals(0, repo.count())
    }

    @Test
    fun `recall_memory lists stored facts`() = runTest {
        repo.add("Mom's number is 555-0100")
        repo.add("I take medication at 8am")
        val result = RecallMemoryTool(repo).execute(JSONObject("{}"))
        assertTrue(result is ToolResult.Success)
        val payload = (result as ToolResult.Success).payload
        assertTrue(payload.contains("555-0100"))
        assertTrue(payload.contains("medication"))
    }

    @Test
    fun `recall_memory filters by query`() = runTest {
        repo.add("Mom's number is 555-0100")
        repo.add("I take medication at 8am")
        val result = RecallMemoryTool(repo).execute(JSONObject("""{"query": "medication"}"""))
        val payload = (result as ToolResult.Success).payload
        assertTrue(payload.contains("medication"))
        assertTrue(!payload.contains("555-0100"))
    }

    @Test
    fun `forget_memory by id deletes one fact`() = runTest {
        val saved = repo.add("temporary fact").getOrThrow()
        repo.add("keep me")
        val result = ForgetMemoryTool(repo).execute(JSONObject("""{"id": ${saved.id}}"""))
        assertTrue(result is ToolResult.Say)
        assertEquals(1, repo.count())
    }

    @Test
    fun `forget_memory all clears everything`() = runTest {
        repo.add("one"); repo.add("two")
        val result = ForgetMemoryTool(repo).execute(JSONObject("""{"all": true}"""))
        assertTrue(result is ToolResult.Say)
        assertEquals(0, repo.count())
    }

    @Test
    fun `forget_memory without id or all fails with guidance`() = runTest {
        val result = ForgetMemoryTool(repo).execute(JSONObject("{}"))
        assertTrue(result is ToolResult.Failure)
    }
}

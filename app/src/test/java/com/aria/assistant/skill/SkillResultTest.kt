package com.aria.assistant.skill

import com.aria.assistant.agent.ToolResult
import com.aria.assistant.permission.PhoneCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillResultTest {

    @Test
    fun `Success holds data`() {
        val result = SkillResult.Success("hello")
        assertEquals("hello", result.data)
        assertTrue(result is SkillResult.Success)
    }

    @Test
    fun `Failure holds reason`() {
        val result = SkillResult.Failure("something went wrong")
        assertEquals("something went wrong", result.reason)
    }

    @Test
    fun `NeedsPermission holds capability`() {
        val result = SkillResult.NeedsPermission(PhoneCapability.CALL)
        assertEquals(PhoneCapability.CALL, result.capability)
    }

    @Test
    fun `NeedsClarification holds question`() {
        val result = SkillResult.NeedsClarification("Did you mean X or Y?")
        assertEquals("Did you mean X or Y?", result.question)
    }

    @Test
    fun `RequiresPremium holds action`() {
        val result = SkillResult.RequiresPremium("make_call")
        assertEquals("make_call", result.action)
    }
}

class ToolResultTest {

    @Test
    fun `fromSkillResult maps Success`() {
        val sr = SkillResult.Success("payload")
        val tr = ToolResult.fromSkillResult(sr)
        assertTrue(tr is ToolResult.Success)
        assertEquals("payload", (tr as ToolResult.Success).payload)
    }

    @Test
    fun `fromSkillResult maps Failure`() {
        val sr = SkillResult.Failure("error")
        val tr = ToolResult.fromSkillResult(sr)
        assertTrue(tr is ToolResult.Failure)
        assertEquals("error", (tr as ToolResult.Failure).reason)
    }

    @Test
    fun `fromSkillResult maps NeedsPermission`() {
        val sr = SkillResult.NeedsPermission(PhoneCapability.CAMERA)
        val tr = ToolResult.fromSkillResult(sr)
        assertTrue(tr is ToolResult.NeedsPermission)
        assertEquals(PhoneCapability.CAMERA, (tr as ToolResult.NeedsPermission).capability)
    }

    @Test
    fun `fromSkillResult maps NeedsClarification`() {
        val sr = SkillResult.NeedsClarification("Which?")
        val tr = ToolResult.fromSkillResult(sr)
        assertTrue(tr is ToolResult.NeedsClarification)
        assertEquals("Which?", (tr as ToolResult.NeedsClarification).question)
    }

    @Test
    fun `fromSkillResult maps RequiresPremium to Say`() {
        val sr = SkillResult.RequiresPremium("make_call")
        val tr = ToolResult.fromSkillResult(sr)
        assertTrue("Expected Say, got ${tr::class.simpleName}", tr is ToolResult.Say)
        assertEquals("That requires a Premium subscription.", (tr as ToolResult.Say).text)
    }

    @Test
    fun `fromSkillResult handles null data`() {
        val sr = SkillResult.Success(null)
        val tr = ToolResult.fromSkillResult(sr)
        assertTrue(tr is ToolResult.Success)
        assertEquals("", (tr as ToolResult.Success).payload)
    }
}

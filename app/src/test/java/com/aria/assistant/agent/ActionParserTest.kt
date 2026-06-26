package com.aria.assistant.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActionParserTest {

    private lateinit var parser: ActionParser

    @Before
    fun setUp() {
        parser = ActionParser()
    }

    @Test
    fun `extractAction from action tag returns JSONObject`() {
        val text = """<action>{"tool":"set_timer","params":{"duration_seconds":600}}</action>"""
        val result = parser.extractAction(text)
        assertNotNull(result)
        assertEquals("set_timer", result!!.optString("tool"))
        assertEquals(600, result.optJSONObject("params")?.optInt("duration_seconds"))
    }

    @Test
    fun `extractAction from legacy JSON block`() {
        val text = """{"action":"web_search","params":{"query":"weather"}}"""
        val result = parser.extractAction(text)
        assertNotNull(result)
        assertEquals("web_search", result!!.optString("action"))
    }

    @Test
    fun `extractAction returns null for plain text`() {
        val result = parser.extractAction("Hello, how are you?")
        assertNull(result)
    }

    @Test
    fun `extractAction with action tag mixed with say tag returns action`() {
        val text = """<say>Setting timer now.</say><action>{"tool":"set_timer","params":{"duration_seconds":600}}</action>"""
        val result = parser.extractAction(text)
        assertNotNull(result)
        assertEquals("set_timer", result!!.optString("tool"))
    }

    @Test
    fun `extractSay from say tag`() {
        val text = """<say>Timer set for 10 minutes.</say>"""
        val result = parser.extractSay(text)
        assertEquals("Timer set for 10 minutes.", result)
    }

    @Test
    fun `extractSay from plain text without tags`() {
        val text = "The weather in London is 15 degrees."
        val result = parser.extractSay(text)
        assertEquals(text, result)
    }

    @Test
    fun `extractSay returns null when only action tag present`() {
        val text = """<action>{"tool":"set_timer","params":{}}</action>"""
        val result = parser.extractSay(text)
        assertNull(result)
    }

    @Test
    fun `extractSay with both tags returns say content`() {
        val text = """<action>{"tool":"set_timer","params":{}}</action><say>Done!</say>"""
        val result = parser.extractSay(text)
        assertEquals("Done!", result)
    }

    @Test
    fun `malformed action JSON returns null`() {
        val text = """<action>not json</action>"""
        val result = parser.extractAction(text)
        assertNull(result)
    }

    @Test
    fun `empty string returns null for both`() {
        assertNull(parser.extractAction(""))
        assertNull(parser.extractSay(""))
    }

    @Test
    fun `case insensitive action tag`() {
        val text = """<ACTION>{"tool":"get_battery","params":{}}</ACTION>"""
        val result = parser.extractAction(text)
        assertNotNull(result)
        assertEquals("get_battery", result!!.optString("tool"))
    }
}

package com.aria.assistant.agent

import com.aria.assistant.billing.BillingManager
import com.aria.assistant.billing.FeatureGate
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.domain.repository.ConversationRepository
import com.aria.assistant.engine.AriaTTS
import com.aria.assistant.engine.LlmEngine
import com.aria.assistant.permission.PhoneCapability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AgentRunnerTest {

    private lateinit var llmEngine: LlmEngine
    private lateinit var deviceContextProvider: DeviceContextProvider
    private lateinit var promptBuilder: PromptBuilder
    private lateinit var actionParser: ActionParser
    private lateinit var ariaTTS: AriaTTS
    private lateinit var billingManager: BillingManager
    private lateinit var featureGate: FeatureGate
    private lateinit var conversationRepo: ConversationRepository

    @Before
    fun setUp() {
        llmEngine = mock()
        deviceContextProvider = mock()
        promptBuilder = mock()
        actionParser = ActionParser()
        ariaTTS = mock()
        billingManager = mock()
        featureGate = mock()
        conversationRepo = mock()

        runBlocking {
            whenever(deviceContextProvider.snapshot()).thenReturn(DeviceContext())
        }
        whenever(promptBuilder.buildSystemPrompt(any(), any())).thenReturn("system prompt")
        whenever(conversationRepo.getRecentMessages()).thenReturn(flow { emit(emptyList()) })
        whenever(billingManager.isPremium).doReturn(MutableStateFlow(true))
        whenever(featureGate.isPremium).doReturn(MutableStateFlow(true))
        whenever(featureGate.isAllowed(any<String>())).doReturn(true)
    }

    @Test
    fun `single action to say flows through`() = runTest {
        val tool = FakeTool("set_timer")
        val runner = createRunner(tool)
        whenever(llmEngine.chat(any()))
            .thenReturn(flow { emit("""<action>{"tool":"set_timer","params":{"duration_seconds":600}}</action>""") })
            .thenReturn(flow { emit("<say>Timer set for 10 minutes.</say>") })

        val states = mutableListOf<AriaState>()
        runner.run("set a 10 minute timer") { states.add(it) }

        assertEquals(1, tool.callCount)
        assertEquals(600, tool.lastParams?.optInt("duration_seconds"))
        verify(ariaTTS).speak("Timer set for 10 minutes.")
        assertTrue(states.contains(AriaState.SPEAKING))
    }

    @Test
    fun `chaining two tools works`() = runTest {
        val smsTool = FakeTool("send_sms")
        val timerTool = FakeTool("set_timer")
        val runner = createRunner(smsTool, timerTool)
        whenever(llmEngine.chat(any()))
            .thenReturn(flow { emit("""<action>{"tool":"send_sms","params":{"contact":"Mom","message":"Late"}}</action>""") })
            .thenReturn(flow { emit("""<action>{"tool":"set_timer","params":{"duration_seconds":900}}</action>""") })
            .thenReturn(flow { emit("<say>Done both.</say>") })

        runner.run("text Mom I'm late and set a 15 minute timer") {}

        assertEquals(1, smsTool.callCount)
        assertEquals(1, timerTool.callCount)
        verify(ariaTTS).speak("Done both.")
    }

    @Test
    fun `NeedsPermission triggers permission flow and stops`() = runTest {
        val tool = FakeTool("read_notifications", result = ToolResult.NeedsPermission(PhoneCapability.CALENDAR))
        val runner = createRunner(tool)
        whenever(llmEngine.chat(any()))
            .thenReturn(flow { emit("""<action>{"tool":"read_notifications","params":{}}</action>""") })

        runner.run("read my notifications") {}

        assertTrue(runner.permissionRequest.replayCache.contains(PhoneCapability.CALENDAR))
        verify(ariaTTS).speak(org.mockito.kotlin.argThat { contains("permission") })
        assertEquals(1, tool.callCount)
    }

    @Test
    fun `NeedsClarification speaks question and stops`() = runTest {
        val tool = FakeTool("make_call", result = ToolResult.NeedsClarification("Did you mean Mom or Mona?"))
        val runner = createRunner(tool)
        whenever(llmEngine.chat(any()))
            .thenReturn(flow { emit("""<action>{"tool":"make_call","params":{"contact":"Mom"}}</action>""") })

        runner.run("call Mom") {}

        verify(ariaTTS).speak("Did you mean Mom or Mona?")
        assertEquals(1, tool.callCount)
    }

    @Test
    fun `premium tool without subscription is gated`() = runTest {
        val tool = FakeTool("make_call", requiresPremium = true)
        whenever(featureGate.isAllowed(any<String>())).doReturn(false)
        val runner = createRunner(tool)
        whenever(llmEngine.chat(any()))
            .thenReturn(flow { emit("""<action>{"tool":"make_call","params":{"contact":"Mom"}}</action>""") })

        runner.run("call Mom") {}

        assertEquals(0, tool.callCount)
        verify(ariaTTS).speak(org.mockito.kotlin.argThat { contains("Premium") })
    }

    @Test
    fun `iteration cap triggers fallback`() = runTest {
        val tool = FakeTool("get_battery")
        val runner = createRunner(tool)
        whenever(llmEngine.chat(any()))
            .thenReturn(flow { emit("""<action>{"tool":"get_battery","params":{}}</action>""") })

        runner.run("keep going") {}

        assertEquals(4, tool.callCount)
        verify(ariaTTS).speak(org.mockito.kotlin.argThat { contains("done what I can") })
    }

    @Test
    fun `Say result from tool is used directly`() = runTest {
        val tool = FakeTool("get_time", result = ToolResult.Say("It's 3:30 PM"))
        val runner = createRunner(tool)
        whenever(llmEngine.chat(any()))
            .thenReturn(flow { emit("""<action>{"tool":"get_time","params":{}}</action>""") })

        runner.run("what time is it") {}

        verify(ariaTTS).speak("It's 3:30 PM")
        assertEquals(1, tool.callCount)
    }

    @Test
    fun `plain text without tags is treated as say`() = runTest {
        val tool = FakeTool("unused")
        val runner = createRunner(tool)
        whenever(llmEngine.chat(any()))
            .thenReturn(flow { emit("The answer is 42.") })

        runner.run("what is the answer") {}

        verify(ariaTTS).speak("The answer is 42.")
        assertEquals(0, tool.callCount)
    }

    @Test
    fun `streamingText is populated on final say`() = runTest {
        val tool = FakeTool("set_timer")
        val runner = createRunner(tool)
        whenever(llmEngine.chat(any()))
            .thenReturn(flow { emit("""<action>{"tool":"set_timer","params":{}}</action>""") })
            .thenReturn(flow { emit("<say>10 minutes, starting now.</say>") })

        runner.run("timer 10 minutes") {}

        assertEquals("10 minutes, starting now.", runner.streamingText.value)
    }

    private fun createRunner(vararg tools: Tool): AgentRunner {
        return AgentRunner(
            llmEngine = llmEngine,
            tools = tools.toSet(),
            deviceContextProvider = deviceContextProvider,
            promptBuilder = promptBuilder,
            actionParser = actionParser,
            ariaTTS = ariaTTS,
            featureGate = featureGate,
            conversationRepo = conversationRepo
        )
    }

    private class FakeTool(
        override val name: String,
        override var requiresPremium: Boolean = false,
        var result: ToolResult = ToolResult.Success("ok")
    ) : Tool {
        override val description: String get() = "Fake $name"
        override val paramSchema: String get() = "{}"
        var lastParams: JSONObject? = null
        var callCount = 0
        override suspend fun execute(params: JSONObject): ToolResult {
            lastParams = params
            callCount++
            return result
        }
    }
}

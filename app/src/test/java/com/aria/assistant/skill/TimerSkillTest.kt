package com.aria.assistant.skill

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TimerSkillTest {

    private lateinit var skill: TimerSkill

    @Before
    fun setUp() {
        skill = TimerSkill(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `setTimer returns success for valid duration`() {
        val result = skill.setTimer(300)
        assertTrue("Expected Success, got ${result::class.simpleName}", result is SkillResult.Success)
        val data = (result as SkillResult.Success).data
        assertTrue(data?.contains("Timer") == true)
    }

    @Test
    fun `setTimer with zero seconds still returns success`() {
        val result = skill.setTimer(0)
        assertTrue(result is SkillResult.Success)
    }

    @Test
    fun `cancelTimer returns success`() {
        val result = skill.cancelTimer()
        assertTrue(result is SkillResult.Success)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AlarmSkillTest {

    private lateinit var skill: AlarmSkill

    @Before
    fun setUp() {
        skill = AlarmSkill(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `setAlarm returns success with valid time`() {
        val result = skill.setAlarm(7, 30, "wake up")
        assertTrue("Expected Success, got ${result::class.simpleName}", result is SkillResult.Success)
        val data = (result as SkillResult.Success).data
        assertTrue(data?.contains("Alarm") == true || data?.contains("alarm") == true)
    }

    @Test
    fun `cancelAlarm returns success`() {
        val result = skill.cancelAlarm("test")
        assertTrue(result is SkillResult.Success)
    }
}

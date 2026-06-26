package com.aria.assistant.skill

import com.aria.assistant.permission.NotificationBridge
import com.aria.assistant.permission.NotifSnapshot
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PermissionResult
import com.aria.assistant.permission.PhoneCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationSkillTest {

    private lateinit var permissionManager: PermissionManager
    private lateinit var notificationBridge: NotificationBridge
    private lateinit var skill: NotificationSkill

    @Before
    fun setUp() {
        permissionManager = mock()
        notificationBridge = mock()
        whenever(permissionManager.ensure(PhoneCapability.NOTIFICATION_LISTENER))
            .thenReturn(PermissionResult.Granted)
        skill = NotificationSkill(permissionManager, notificationBridge)
    }

    @Test
    fun `readActive returns no notifications when empty`() {
        whenever(notificationBridge.getActive(any())).thenReturn(emptyList())
        val result = skill.readActive(null)
        assertTrue("Expected Success, got ${result::class.simpleName}", result is SkillResult.Success)
        val data = (result as SkillResult.Success).data
        assertTrue(data?.contains("No notifications") == true)
    }

    @Test
    fun `readActive with filter returns matching notifications`() {
        whenever(notificationBridge.getActive("messages")).thenReturn(
            listOf(
                NotifSnapshot("k1", "com.example", null, 1, "Hi", "Hello there", true, null)
            )
        )
        val result = skill.readActive("messages")
        assertTrue(result is SkillResult.Success)
        val data = (result as SkillResult.Success).data
        assertTrue(data?.contains("com.example") == true)
        assertTrue(data?.contains("Hi") == true)
    }

    @Test
    fun `readActive returns NeedsPermission when denied`() {
        whenever(permissionManager.ensure(PhoneCapability.NOTIFICATION_LISTENER))
            .thenReturn(PermissionResult.Denied(false))
        val result = skill.readActive(null)
        assertTrue("Expected NeedsPermission, got ${result::class.simpleName}",
            result is SkillResult.NeedsPermission)
        assertEquals(PhoneCapability.NOTIFICATION_LISTENER,
            (result as SkillResult.NeedsPermission).capability)
    }

    @Test
    fun `reply returns NeedsPermission when denied`() {
        whenever(permissionManager.ensure(PhoneCapability.NOTIFICATION_LISTENER))
            .thenReturn(PermissionResult.Denied(false))
        val result = skill.reply("k1", "hello")
        assertTrue(result is SkillResult.NeedsPermission)
    }

    @Test
    fun `dismiss returns NeedsPermission when denied`() {
        whenever(permissionManager.ensure(PhoneCapability.NOTIFICATION_LISTENER))
            .thenReturn(PermissionResult.Denied(false))
        val result = skill.dismiss("k1")
        assertTrue(result is SkillResult.NeedsPermission)
    }
}

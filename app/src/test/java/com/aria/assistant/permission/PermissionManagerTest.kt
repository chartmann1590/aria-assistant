package com.aria.assistant.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class PermissionManagerTest {

    private lateinit var context: Context
    private lateinit var pm: PackageManager
    private lateinit var permissionManager: PermissionManager

    @Before
    fun setUp() {
        context = mock()
        pm = mock()
        whenever(context.packageManager).thenReturn(pm)
        whenever(context.packageName).thenReturn("com.aria.assistant")
        whenever(context.checkSelfPermission(any())).thenReturn(PackageManager.PERMISSION_DENIED)
        permissionManager = PermissionManager(context)
    }

    @Test
    fun `status CALL granted when checkSelfPermission returns GRANTED`() {
        whenever(context.checkSelfPermission(android.Manifest.permission.CALL_PHONE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        val result = permissionManager.status(PhoneCapability.CALL)
        assertTrue("Expected Granted, got ", result is PermissionResult.Granted)
    }

    @Test
    fun `status CALL denied when checkSelfPermission returns DENIED`() {
        val result = permissionManager.status(PhoneCapability.CALL)
        assertTrue("Expected Denied, got ", result is PermissionResult.Denied)
        assertFalse((result as PermissionResult.Denied).permanent)
    }

    @Test
    fun `status CAMERA denied by default`() {
        val result = permissionManager.status(PhoneCapability.CAMERA)
        assertTrue("Expected Denied, got ", result is PermissionResult.Denied)
    }

    @Test
    fun `runtimePerms returns CALL_PHONE for CALL`() {
        val perms = permissionManager.runtimePerms(PhoneCapability.CALL)
        assertEquals(listOf(android.Manifest.permission.CALL_PHONE), perms)
    }

    @Test
    fun `runtimePerms returns FINE and COARSE location for LOCATION`() {
        val perms = permissionManager.runtimePerms(PhoneCapability.LOCATION)
        assertTrue(perms.contains(android.Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(perms.contains(android.Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Test
    fun `ensure delegates to status`() {
        assertEquals(permissionManager.status(PhoneCapability.CALL), permissionManager.ensure(PhoneCapability.CALL))
        assertEquals(permissionManager.status(PhoneCapability.WRITE_SETTINGS), permissionManager.ensure(PhoneCapability.WRITE_SETTINGS))
    }

    @Test
    fun `requestIntent WRITE_SETTINGS returns ACTION_MANAGE_WRITE_SETTINGS with package URI`() {
        val intent = permissionManager.requestIntent(PhoneCapability.WRITE_SETTINGS)
        assertEquals("Expected ACTION_MANAGE_WRITE_SETTINGS",
            Settings.ACTION_MANAGE_WRITE_SETTINGS, intent.action)
        assertNotNull("Expected data URI", intent.data)
        assertTrue("Expected package in URI", intent.data.toString().contains("com.aria.assistant"))
    }

    @Test
    fun `requestIntent BATTERY_OPT returns ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`() {
        val intent = permissionManager.requestIntent(PhoneCapability.BATTERY_OPT)
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
    }

    @Test
    fun `requestIntent USAGE_ACCESS returns ACTION_USAGE_ACCESS_SETTINGS`() {
        val intent = permissionManager.requestIntent(PhoneCapability.USAGE_ACCESS)
        assertEquals(Settings.ACTION_USAGE_ACCESS_SETTINGS, intent.action)
    }

    @Test
    fun `requestIntent NOTIFICATION_LISTENER returns ACTION_NOTIFICATION_LISTENER_SETTINGS`() {
        val intent = permissionManager.requestIntent(PhoneCapability.NOTIFICATION_LISTENER)
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, intent.action)
    }

    @Test
    fun `requestIntent ACCESSIBILITY returns ACTION_ACCESSIBILITY_SETTINGS`() {
        val intent = permissionManager.requestIntent(PhoneCapability.ACCESSIBILITY)
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, intent.action)
    }

    @Test
    fun `every capability has a registered spec`() {
        for (cap in PhoneCapability.entries) {
            val status = permissionManager.status(cap)
            assertNotNull("Status should not be null for ", status)
        }
    }

    @Test
    fun `APP_LAUNCH is always granted`() {
        val result = permissionManager.status(PhoneCapability.APP_LAUNCH)
        assertTrue("APP_LAUNCH should be manifest-only and always granted",
            result is PermissionResult.Granted)
    }

}

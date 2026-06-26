package com.aria.assistant.skill

import com.aria.assistant.permission.AccessibilityBridge
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.permission.PermissionResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenControlSkill @Inject constructor(
    private val permissionManager: PermissionManager,
    private val accessibilityBridge: AccessibilityBridge
) {
    fun readScreen(detail: String?): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.ACCESSIBILITY)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.ACCESSIBILITY)
        }
        return accessibilityBridge.readScreen()
    }

    fun clickLabel(label: String): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.ACCESSIBILITY)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.ACCESSIBILITY)
        }
        return accessibilityBridge.clickLabel(label)
    }

    fun scroll(direction: String): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.ACCESSIBILITY)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.ACCESSIBILITY)
        }
        return accessibilityBridge.scroll(direction)
    }
}

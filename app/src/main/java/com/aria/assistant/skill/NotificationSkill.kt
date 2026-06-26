package com.aria.assistant.skill

import com.aria.assistant.permission.NotificationBridge
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.permission.PermissionResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSkill @Inject constructor(
    private val permissionManager: PermissionManager,
    private val notificationBridge: NotificationBridge
) {
    fun readActive(filter: String?): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.NOTIFICATION_LISTENER)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.NOTIFICATION_LISTENER)
        }
        val notifs = notificationBridge.getActive(filter)
        if (notifs.isEmpty()) {
            return SkillResult.Success("No notifications" + if (filter != null) " matching '$filter'" else "" + " found")
        }
        val sb = StringBuilder()
        for (n in notifs) {
            sb.append("From ${n.packageName}: ")
            if (n.title.isNotBlank()) sb.append(n.title).append(" - ")
            sb.append(n.text).append(". ")
        }
        return SkillResult.Success(sb.toString().trimEnd())
    }

    fun reply(notificationKey: String, replyText: String): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.NOTIFICATION_LISTENER)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.NOTIFICATION_LISTENER)
        }
        return notificationBridge.reply(notificationKey, replyText)
    }

    fun dismiss(notificationKey: String): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.NOTIFICATION_LISTENER)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.NOTIFICATION_LISTENER)
        }
        return notificationBridge.dismiss(notificationKey)
    }
}

package com.aria.assistant.skill

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import com.aria.assistant.permission.NotificationBridge
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.permission.PermissionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallSkill @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager,
    private val notificationBridge: NotificationBridge
) {
    fun makeCall(contactName: String): SkillResult<String> {
        val callPerm = permissionManager.ensure(PhoneCapability.CALL)
        if (callPerm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.CALL)
        }
        val contactsPerm = permissionManager.ensure(PhoneCapability.CONTACTS)
        if (contactsPerm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.CONTACTS)
        }
        val phoneNumber = resolveContact(contactName)
        if (phoneNumber == null) {
            return SkillResult.Failure(enrichFailure(contactName))
        }
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return SkillResult.Success("Calling $contactName")
    }

    fun readLastCall(limit: Int?): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.CALL)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.CALL)
        }
        val count = limit ?: 5
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE
        )
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC LIMIT $count"
        )
        return cursor?.use {
            if (it.count == 0) return@use SkillResult.Success("No call history found")
            val sb = StringBuilder()
            var i = 0
            while (it.moveToNext() && i < count) {
                val name = it.getString(1) ?: it.getString(0) ?: "Unknown"
                val type = when (it.getInt(2)) {
                    CallLog.Calls.INCOMING_TYPE -> "received"
                    CallLog.Calls.OUTGOING_TYPE -> "dialed"
                    CallLog.Calls.MISSED_TYPE -> "missed"
                    else -> "call"
                }
                sb.append("$name ($type), ")
                i++
            }
            SkillResult.Success("Recent calls: ${sb.toString().trimEnd(',', ' ')}")
        } ?: SkillResult.Failure("Could not read call log")
    }

    fun answerCall(): SkillResult<String> {
        val callNotifs = notificationBridge.getActive("phone")
        if (callNotifs.isEmpty()) {
            return SkillResult.Failure("No incoming call found to answer")
        }
        val notif = callNotifs.first()
        val answerAction = notif.notification?.actions?.firstOrNull { action ->
            action.actionIntent != null &&
            (action.title?.toString()?.contains("answer", ignoreCase = true) == true ||
             action.title?.toString()?.contains("accept", ignoreCase = true) == true)
        }
        if (answerAction != null) {
            try {
                answerAction.actionIntent?.send()
                return SkillResult.Success("Answering call")
            } catch (e: android.app.PendingIntent.CanceledException) {
                return SkillResult.Failure("Failed to answer call")
            }
        }
        return SkillResult.Failure("Could not find answer action on the incoming call notification")
    }

    fun rejectCall(): SkillResult<String> {
        val callNotifs = notificationBridge.getActive("phone")
        if (callNotifs.isEmpty()) {
            return SkillResult.Failure("No incoming call found to reject")
        }
        val notif = callNotifs.first()
        val rejectAction = notif.notification?.actions?.firstOrNull { action ->
            action.actionIntent != null &&
            (action.title?.toString()?.contains("decline", ignoreCase = true) == true ||
             action.title?.toString()?.contains("dismiss", ignoreCase = true) == true ||
             action.title?.toString()?.contains("reject", ignoreCase = true) == true)
        }
        if (rejectAction != null) {
            try {
                rejectAction.actionIntent?.send()
                return SkillResult.Success("Rejecting call")
            } catch (e: android.app.PendingIntent.CanceledException) {
                return SkillResult.Failure("Failed to reject call")
            }
        }
        return SkillResult.Failure("Could not find reject action on the incoming call notification")
    }

    private fun resolveContact(name: String): String? {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT 10"
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun suggestContacts(name: String): String? {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT 5"
        )
        return cursor?.use {
            if (it.count == 0) return@use null
            val names = mutableListOf<String>()
            while (it.moveToNext()) {
                val cn = it.getString(0) ?: continue
                if (cn !in names) names.add(cn)
            }
            if (names.isEmpty()) null else names.joinToString(", ")
        }
    }

    private fun enrichFailure(name: String): String {
        val suggestions = suggestContacts(name)
        return if (suggestions != null) {
            "I couldn't find a contact named '$name'. Did you mean one of: $suggestions?"
        } else {
            "I couldn't find a contact named $name"
        }
    }
}

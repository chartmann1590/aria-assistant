package com.aria.assistant.skill

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.permission.PermissionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageSkill @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {
    fun sendSms(contactName: String, body: String): SkillResult<String> {
        val smsPerm = permissionManager.ensure(PhoneCapability.SMS)
        if (smsPerm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.SMS)
        }
        val contactsPerm = permissionManager.ensure(PhoneCapability.CONTACTS)
        if (contactsPerm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.CONTACTS)
        }
        val phoneNumber = resolveContact(contactName)
        if (phoneNumber == null) {
            return SkillResult.Failure(enrichFailure(contactName))
        }
        @Suppress("DEPRECATION")
        SmsManager.getDefault().sendTextMessage(
            phoneNumber,
            null,
            body,
            null,
            null
        )
        return SkillResult.Success("Message sent to $contactName")
    }

    fun readSmsInbox(limit: Int?): SkillResult<String> {
        val readPerm = permissionManager.ensure(PhoneCapability.READ_SMS)
        if (readPerm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.READ_SMS)
        }
        val count = limit ?: 5
        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.Inbox.DATE} DESC LIMIT $count"
        )
        return cursor?.use {
            if (it.count == 0) return@use SkillResult.Success("No messages in inbox")
            val sb = StringBuilder()
            var i = 0
            while (it.moveToNext() && i < count) {
                val address = it.getString(0) ?: "Unknown"
                val body = it.getString(1) ?: ""
                sb.append("From $address: ${body.take(80)}. ")
                i++
            }
            SkillResult.Success("SMS inbox: ${sb.toString().trimEnd()}")
        } ?: SkillResult.Failure("Could not read SMS inbox")
    }

    fun shareToWhatsApp(contactName: String, message: String): SkillResult<String> {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, message)
            setType("text/plain")
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (sendIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(sendIntent)
            return SkillResult.Success("Opening WhatsApp to send message")
        }
        return SkillResult.Failure("WhatsApp is not installed")
    }

    fun shareToTelegram(contactName: String, message: String): SkillResult<String> {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, message)
            setType("text/plain")
            setPackage("org.telegram.messenger")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (sendIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(sendIntent)
            return SkillResult.Success("Opening Telegram to send message")
        }
        return SkillResult.Failure("Telegram is not installed")
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

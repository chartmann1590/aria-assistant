package com.aria.assistant.skill

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.permission.PermissionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSkill @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {
    fun createEvent(title: String, startMs: Long, endMs: Long?, location: String?): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.CALENDAR)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.CALENDAR)
        }
        val endTime = endMs ?: (startMs + 3600000)
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, 1)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endTime)
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            if (location != null) put(CalendarContract.Events.EVENT_LOCATION, location)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return if (uri != null) {
            SkillResult.Success("Calendar event '$title' created")
        } else {
            SkillResult.Failure("Failed to create calendar event")
        }
    }

    fun listEvents(fromMs: Long?, toMs: Long?): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.CALENDAR)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.CALENDAR)
        }
        val now = System.currentTimeMillis()
        val start = fromMs ?: now
        val end = toMs ?: (now + 604800000L)
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION
        )
        val cursor = CalendarContract.Instances.query(
            context.contentResolver, projection, start, end
        )
        return cursor?.use {
            if (it.count == 0) return@use SkillResult.Success("No events found in that period")
            val sb = StringBuilder()
            while (it.moveToNext()) {
                val title = it.getString(0) ?: "Untitled"
                val begin = it.getLong(1)
                val location = it.getString(3) ?: ""
                sb.append(title)
                if (location.isNotBlank()) sb.append(" at $location")
                sb.append("; ")
            }
            SkillResult.Success(sb.toString().trimEnd(';', ' '))
        } ?: SkillResult.Failure("Could not read calendar")
    }

    fun setReminder(eventId: String?, minutesBefore: Int, label: String?): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.CALENDAR)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.CALENDAR)
        }
        val title = label ?: "Reminder"
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.MINUTES, minutesBefore)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        if (eventId != null) {
            values.put(CalendarContract.Reminders.EVENT_ID, eventId.toLongOrNull() ?: 0)
        }
        val uri = context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        return if (uri != null) {
            SkillResult.Success("Reminder set for $title in $minutesBefore minutes")
        } else {
            SkillResult.Failure("Failed to set reminder")
        }
    }
}

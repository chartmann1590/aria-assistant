package com.aria.assistant.agent

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import com.aria.assistant.skill.AlarmSkill
import com.aria.assistant.skill.AppLaunchSkill
import com.aria.assistant.skill.CalendarSkill
import com.aria.assistant.skill.CallSkill
import com.aria.assistant.skill.CameraSkill
import com.aria.assistant.skill.DeviceInfoSkill
import com.aria.assistant.skill.LocationSkill
import com.aria.assistant.skill.MediaSkill
import com.aria.assistant.skill.MessageSkill
import com.aria.assistant.skill.NotificationSkill
import com.aria.assistant.skill.ScreenControlSkill
import com.aria.assistant.skill.SettingsSkill
import com.aria.assistant.skill.TimerSkill
import com.aria.assistant.skill.WebSearchSkill
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetTimerTool @Inject constructor(
    private val timerSkill: TimerSkill
) : Tool {
    override val name = "set_timer"
    override val description = "Set a countdown timer"
    override val paramSchema = """{"duration_seconds": 600}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(timerSkill.setTimer(params.optInt("duration_seconds", 0)))
    }
}

@Singleton
class SetAlarmTool @Inject constructor(
    private val alarmSkill: AlarmSkill
) : Tool {
    override val name = "set_alarm"
    override val description = "Set an alarm for a specific time"
    override val paramSchema = """{"hour": 7, "minute": 30, "label": "wake up"}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(alarmSkill.setAlarm(
            params.optInt("hour"), params.optInt("minute"), params.optString("label")
        ))
    }
}

@Singleton
class WebSearchTool @Inject constructor(
    private val webSearchSkill: WebSearchSkill
) : Tool {
    override val name = "web_search"
    override val description = "Search the internet for current information"
    override val paramSchema = """{"query": "weather in London"}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(webSearchSkill.search(params.optString("query", "")))
    }
}

@Singleton
class ListCalendarEventsTool @Inject constructor(
    private val calendarSkill: CalendarSkill
) : Tool {
    override val name = "list_calendar_events"
    override val description = "List upcoming calendar events in a time range"
    override val paramSchema = """{"from_ms": 1700000000000, "to_ms": 1700086400000}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(calendarSkill.listEvents(
            if (params.has("from_ms")) params.optLong("from_ms") else null,
            if (params.has("to_ms")) params.optLong("to_ms") else null
        ))
    }
}

@Singleton
class GetLocationTool @Inject constructor(
    private val locationSkill: LocationSkill
) : Tool {
    override val name = "get_location"
    override val description = "Get the user's current location"
    override val paramSchema = """{}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(locationSkill.currentLocation())
    }
}

@Singleton
class NavigateToTool @Inject constructor(
    private val locationSkill: LocationSkill
) : Tool {
    override val name = "navigate_to"
    override val description = "Open navigation directions to a place"
    override val paramSchema = """{"place": "nearest coffee shop"}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(locationSkill.navigateTo(params.optString("place", "")))
    }
}

@Singleton
class MediaControlTool @Inject constructor(
    private val mediaSkill: MediaSkill
) : Tool {
    override val name = "media_control"
    override val description = "Control media playback: play, pause, next, prev, now_playing"
    override val paramSchema = """{"action": "play|pause|next|prev|now_playing"}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        val action = params.optString("action", "").lowercase()
        val result = when (action) {
            "play", "pause", "play_pause" -> mediaSkill.playPause()
            "next", "skip" -> mediaSkill.next()
            "prev", "previous" -> mediaSkill.prev()
            "now_playing", "now playing", "status" -> mediaSkill.nowPlaying()
            else -> com.aria.assistant.skill.SkillResult.Failure("Unknown media action: $action")
        }
        return ToolResult.fromSkillResult(result)
    }
}

@Singleton
class LaunchAppTool @Inject constructor(
    private val appLaunchSkill: AppLaunchSkill
) : Tool {
    override val name = "launch_app"
    override val description = "Open an installed app by name"
    override val paramSchema = """{"app": "Spotify"}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(appLaunchSkill.open(params.optString("app", "")))
    }
}

@Singleton
class ListAppsTool @Inject constructor(
    private val appLaunchSkill: AppLaunchSkill
) : Tool {
    override val name = "list_apps"
    override val description = "List installed apps, optionally filtered"
    override val paramSchema = """{"filter": "music"}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(appLaunchSkill.listApps(params.optString("filter", null)))
    }
}

@Singleton
class GetBatteryTool @Inject constructor(
    private val deviceInfoSkill: DeviceInfoSkill
) : Tool {
    override val name = "get_battery"
    override val description = "Get current battery level and charging status"
    override val paramSchema = """{}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(deviceInfoSkill.battery())
    }
}

@Singleton
class GetTimeTool @Inject constructor(
    private val deviceInfoSkill: DeviceInfoSkill
) : Tool {
    override val name = "get_time"
    override val description = "Get the current date and time"
    override val paramSchema = """{}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(deviceInfoSkill.time())
    }
}

@Singleton
class ReadSmsTool @Inject constructor(
    private val messageSkill: MessageSkill
) : Tool {
    override val name = "read_sms"
    override val description = "Read recent SMS messages from inbox"
    override val paramSchema = """{"limit": 5}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(messageSkill.readSmsInbox(
            if (params.has("limit")) params.optInt("limit") else null
        ))
    }
}

@Singleton
class DismissNotificationTool @Inject constructor(
    private val notificationSkill: NotificationSkill
) : Tool {
    override val name = "dismiss_notification"
    override val description = "Dismiss a notification by key"
    override val paramSchema = """{"notification_key": "key123"}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(notificationSkill.dismiss(params.optString("notification_key", "")))
    }
}

@Singleton
class ResolveContactTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {
    override val name = "resolve_contact"
    override val description = "Search contacts by name and return matching candidates"
    override val paramSchema = """{"name": "Mom"}"""
    override val requiresPremium = false
    override suspend fun execute(params: JSONObject): ToolResult {
        val name = params.optString("name", "")
        if (name.isBlank()) return ToolResult.Failure("Please provide a contact name to search for")
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT 10"
        )
        return cursor?.use {
            if (it.count == 0) {
                ToolResult.Success("")
            } else {
                val matches = mutableListOf<String>()
                while (it.moveToNext()) {
                    val cn = it.getString(0) ?: "?"
                    val num = it.getString(1) ?: "?"
                    if (cn !in matches) matches.add(cn)
                }
                if (matches.isEmpty()) {
                    ToolResult.Success("")
                } else {
                    val list = matches.joinToString(", ")
                    ToolResult.Success("Contacts matching '$name': $list")
                }
            }
        } ?: ToolResult.Failure("Could not access contacts")
    }
}

// --- Premium tools ---

@Singleton
class MakeCallTool @Inject constructor(
    private val callSkill: CallSkill
) : Tool {
    override val name = "make_call"
    override val description = "Place a phone call to a contact"
    override val paramSchema = """{"contact": "Mom"}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(callSkill.makeCall(params.optString("contact", "")))
    }
}

@Singleton
class SendSmsTool @Inject constructor(
    private val messageSkill: MessageSkill
) : Tool {
    override val name = "send_sms"
    override val description = "Send an SMS text message to a contact"
    override val paramSchema = """{"contact": "Mom", "message": "On my way"}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(messageSkill.sendSms(
            params.optString("contact", ""), params.optString("message", "")
        ))
    }
}

@Singleton
class AdjustSettingTool @Inject constructor(
    private val settingsSkill: SettingsSkill
) : Tool {
    override val name = "adjust_setting"
    override val description = "Change a device setting: brightness, volume, wifi, bluetooth, dnd, airplane_mode, battery_saver"
    override val paramSchema = """{"setting": "brightness", "value": "50%"}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(settingsSkill.changeSetting(
            params.optString("setting", ""), params.optString("value", "")
        ))
    }
}

@Singleton
class CreateCalendarEventTool @Inject constructor(
    private val calendarSkill: CalendarSkill
) : Tool {
    override val name = "create_calendar_event"
    override val description = "Create a new calendar event"
    override val paramSchema = """{"title": "Dentist", "start_ms": 1700000000000, "end_ms": 1700003600000, "location": "Clinic"}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(calendarSkill.createEvent(
            params.optString("title", ""),
            params.optLong("start_ms", 0),
            if (params.has("end_ms")) params.optLong("end_ms") else null,
            params.optString("location", null)
        ))
    }
}

@Singleton
class SetReminderTool @Inject constructor(
    private val calendarSkill: CalendarSkill
) : Tool {
    override val name = "set_reminder"
    override val description = "Set a reminder for an event or task"
    override val paramSchema = """{"label": "Buy groceries", "minutes_before": 15}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(calendarSkill.setReminder(
            params.optString("event_id", null),
            params.optInt("minutes_before", 15),
            params.optString("label", null)
        ))
    }
}

@Singleton
class ReadNotificationsTool @Inject constructor(
    private val notificationSkill: NotificationSkill
) : Tool {
    override val name = "read_notifications"
    override val description = "Read active notifications, optionally filtered by app"
    override val paramSchema = """{"filter": "messages"}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(notificationSkill.readActive(params.optString("filter", null)))
    }
}

@Singleton
class ReplyNotificationTool @Inject constructor(
    private val notificationSkill: NotificationSkill
) : Tool {
    override val name = "reply_notification"
    override val description = "Reply to a notification with a text message"
    override val paramSchema = """{"notification_key": "key", "reply_text": "On my way"}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(notificationSkill.reply(
            params.optString("notification_key", ""), params.optString("reply_text", "")
        ))
    }
}

@Singleton
class TakePhotoTool @Inject constructor(
    private val cameraSkill: CameraSkill
) : Tool {
    override val name = "take_photo"
    override val description = "Take a photo using the camera"
    override val paramSchema = """{"label": "document"}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(cameraSkill.takePhoto(params.optString("label", null)))
    }
}

@Singleton
class GetLatestPhotoTool @Inject constructor(
    private val cameraSkill: CameraSkill
) : Tool {
    override val name = "get_latest_photo"
    override val description = "Get information about the latest photos"
    override val paramSchema = """{"count": 3}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(cameraSkill.getLatestPhoto(
            if (params.has("count")) params.optInt("count") else null
        ))
    }
}

@Singleton
class ReadScreenTool @Inject constructor(
    private val screenControlSkill: ScreenControlSkill
) : Tool {
    override val name = "read_screen"
    override val description = "Read the text currently displayed on screen"
    override val paramSchema = """{}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(screenControlSkill.readScreen(params.optString("detail", null)))
    }
}

@Singleton
class ClickOnTool @Inject constructor(
    private val screenControlSkill: ScreenControlSkill
) : Tool {
    override val name = "click_on"
    override val description = "Click on a UI element by its visible label"
    override val paramSchema = """{"label": "Send"}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(screenControlSkill.clickLabel(params.optString("label", "")))
    }
}

@Singleton
class ScrollTool @Inject constructor(
    private val screenControlSkill: ScreenControlSkill
) : Tool {
    override val name = "scroll"
    override val description = "Scroll the screen in a direction: up, down, left, right"
    override val paramSchema = """{"direction": "down"}"""
    override val requiresPremium = true
    override suspend fun execute(params: JSONObject): ToolResult {
        return ToolResult.fromSkillResult(screenControlSkill.scroll(
            params.optString("direction", "down")
        ))
    }
}

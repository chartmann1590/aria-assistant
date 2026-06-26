package com.aria.assistant.engine

import com.aria.assistant.agent.AgentRunner
import com.aria.assistant.domain.model.AriaIntent
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.permission.PhoneCapability
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentRouter @Inject constructor(
    private val agentRunner: AgentRunner
) {
    val streamingText: StateFlow<String> = agentRunner.streamingText
    val permissionRequest: SharedFlow<PhoneCapability> = agentRunner.permissionRequest

    fun setSessionId(id: String) {
        agentRunner.setSessionId(id)
    }

    suspend fun process(transcript: String, onStateChange: (AriaState) -> Unit) {
        agentRunner.run(transcript, onStateChange)
    }

    fun stopSpeaking() {
        agentRunner.stopSpeaking()
    }

    fun resolve(text: String): AriaIntent {
        val jsonBlock = extractJsonBlock(text)
            ?: return AriaIntent.GeneralResponse(text = text, spokenText = text)
        return try {
            val json = JSONObject(jsonBlock)
            val p = json.optJSONObject("params") ?: JSONObject()
            when (json.optString("action", "")) {
                "set_timer" -> AriaIntent.SetTimer(p.optInt("duration_seconds", 0))
                "set_alarm" -> AriaIntent.SetAlarm(p.optInt("hour", 0), p.optInt("minute", 0), p.optString("label"))
                "make_call" -> AriaIntent.MakeCall(p.optString("contact", ""))
                "send_sms" -> AriaIntent.SendSms(p.optString("contact", ""), p.optString("message", ""))
                "adjust_setting" -> AriaIntent.AdjustSetting(p.optString("setting", ""), p.optString("value", ""))
                "web_search" -> AriaIntent.WebSearch(p.optString("query", ""))
                "create_calendar_event" -> AriaIntent.CreateCalendarEvent(
                    p.optString("title", ""), p.optLong("start_ms", 0),
                    if (p.has("end_ms")) p.optLong("end_ms") else null,
                    p.optString("location", null)
                )
                "list_calendar_events" -> AriaIntent.ListCalendarEvents(
                    if (p.has("from_ms")) p.optLong("from_ms") else null,
                    if (p.has("to_ms")) p.optLong("to_ms") else null
                )
                "set_reminder" -> AriaIntent.SetReminder(
                    p.optString("event_id", null),
                    p.optInt("minutes_before", 15),
                    p.optString("label", null)
                )
                "read_notifications" -> AriaIntent.ReadNotifications(p.optString("filter", null))
                "reply_notification" -> AriaIntent.ReplyNotification(
                    p.optString("notification_key", ""), p.optString("reply_text", "")
                )
                "get_location" -> AriaIntent.GetLocation(p.optString("query", null))
                "navigate_to" -> AriaIntent.NavigateTo(p.optString("place", ""))
                "take_photo" -> AriaIntent.TakePhoto(p.optString("label", null))
                "get_latest_photo" -> AriaIntent.GetLatestPhoto(if (p.has("count")) p.optInt("count") else null)
                "media_control" -> AriaIntent.MediaControl(p.optString("action", ""))
                "launch_app" -> AriaIntent.LaunchApp(p.optString("app", ""))
                "list_apps" -> AriaIntent.ListApps(p.optString("filter", null))
                "read_screen" -> AriaIntent.ReadScreen(p.optString("detail", null))
                "click_on" -> AriaIntent.ClickOn(p.optString("label", ""))
                "scroll" -> AriaIntent.Scroll(p.optString("direction", "down"), p.optString("container", null))
                "get_battery" -> AriaIntent.GetBattery(p.optString("detail", null))
                "get_time" -> AriaIntent.GetTime(p.optString("format", null))
                else -> AriaIntent.Unrecognized
            }
        } catch (_: Exception) {
            AriaIntent.GeneralResponse(text = text, spokenText = text)
        }
    }

    fun parseIntent(response: String): AriaIntent = resolve(response)

    private fun extractJsonBlock(text: String): String? {
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    fun observeNavigation(): NavigationEvent? = null

    data class NavigationEvent(val destination: String)
}

package com.aria.assistant.billing

import com.aria.assistant.BuildConfig
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class Feature(val toolNames: Set<String>, val isPremium: Boolean) {
    GENERAL_QA(emptySet(), false),
    TIMER_ALARM(setOf("set_timer", "set_alarm", "cancel_timer", "cancel_alarm"), false),
    WAKE_WORD(emptySet(), false),
    DEFAULT_VOICE(emptySet(), false),
    WEB_SEARCH(setOf("web_search"), false),
    CALENDAR_READ(setOf("list_calendar_events"), false),
    LOCATION(setOf("get_location", "navigate_to"), false),
    MEDIA_CONTROL(setOf("media_control"), false),
    APP_LAUNCH(setOf("launch_app", "list_apps"), false),
    DEVICE_INFO(setOf("get_battery", "get_time", "get_storage"), false),
    SMS_READ(setOf("read_sms"), false),
    NOTIFICATION_DISMISS(setOf("dismiss_notification"), false),
    CONTACT_RESOLVE(setOf("resolve_contact"), false),
    NEARBY_SEARCH(setOf("nearby_search"), false),
    REVERSE_GEOCODE(setOf("reverse_geocode"), false),
    SHARE(setOf("share_to_whatsapp", "share_to_telegram"), false),
    FLASHLIGHT(setOf("flashlight"), false),
    CLIPBOARD(setOf("clipboard_read", "clipboard_write"), false),
    EMAIL(setOf("email_compose"), false),
    UNIT_CONVERSION(setOf("convert"), false),
    CALL_HISTORY(setOf("read_last_calls"), true),
    CALL_CONTROL(setOf("answer_call", "reject_call"), true),
    PREMIUM_VOICES(emptySet(), true),
    GEMMA_E4B(emptySet(), true),
    PHONE_CALLS(setOf("make_call"), true),
    SMS_SEND(setOf("send_sms"), true),
    SETTINGS_CONTROL(setOf("adjust_setting"), true),
    CALENDAR_WRITE(setOf("create_calendar_event", "set_reminder"), true),
    NOTIFICATIONS_READ_REPLY(setOf("read_notifications", "reply_notification"), true),
    CAMERA(setOf("take_photo", "get_latest_photo"), true),
    SCREEN_CONTROL(setOf("read_screen", "click_on", "scroll"), true);

    companion object {
        private val toolToFeature: Map<String, Feature> = entries.flatMap { f ->
            f.toolNames.map { t -> t to f }
        }.toMap()

        fun featureForTool(toolName: String): Feature? = toolToFeature[toolName]
    }
}

@Singleton
class FeatureGate @Inject constructor(
    private val billingManager: BillingManager
) {
    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    fun isAllowed(toolName: String): Boolean {
        if (!BuildConfig.ENABLE_ACCESSIBILITY_AUTOMATION && toolName in accessibilityTools) return false
        if (!BuildConfig.ENABLE_RESTRICTED_MESSAGING && toolName in restrictedMessagingTools) return false
        val feature = Feature.featureForTool(toolName) ?: return true
        return !feature.isPremium || billingManager.isPremium.value
    }

    fun isAllowed(feature: Feature): Boolean {
        return isShipped(feature) && (!feature.isPremium || billingManager.isPremium.value)
    }

    val premiumFeatures: List<Feature>
        get() = Feature.entries.filter { feature ->
            feature.isPremium && isShipped(feature)
        }

    private fun isShipped(feature: Feature): Boolean = when (feature) {
        Feature.SCREEN_CONTROL -> BuildConfig.ENABLE_ACCESSIBILITY_AUTOMATION
        Feature.SMS_READ, Feature.SMS_SEND, Feature.CALL_HISTORY, Feature.CALL_CONTROL ->
            BuildConfig.ENABLE_RESTRICTED_MESSAGING
        else -> true
    }

    private companion object {
        val accessibilityTools = setOf("read_screen", "click_on", "scroll")
        val restrictedMessagingTools = setOf(
            "read_sms", "send_sms", "read_last_calls", "answer_call", "reject_call"
        )
    }
}

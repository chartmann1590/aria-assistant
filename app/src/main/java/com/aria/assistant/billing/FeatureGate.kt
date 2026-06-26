package com.aria.assistant.billing

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class Feature(val toolNames: Set<String>, val isPremium: Boolean) {
    GENERAL_QA(emptySet(), false),
    TIMER_ALARM(setOf("set_timer", "set_alarm"), false),
    WAKE_WORD(emptySet(), false),
    DEFAULT_VOICE(emptySet(), false),
    WEB_SEARCH(setOf("web_search"), false),
    CALENDAR_READ(setOf("list_calendar_events"), false),
    LOCATION(setOf("get_location", "navigate_to"), false),
    MEDIA_CONTROL(setOf("media_control"), false),
    APP_LAUNCH(setOf("launch_app", "list_apps"), false),
    DEVICE_INFO(setOf("get_battery", "get_time"), false),
    SMS_READ(setOf("read_sms"), false),
    NOTIFICATION_DISMISS(setOf("dismiss_notification"), false),
    CONTACT_RESOLVE(setOf("resolve_contact"), false),
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
        val feature = Feature.featureForTool(toolName) ?: return true
        return !feature.isPremium || billingManager.isPremium.value
    }

    fun isAllowed(feature: Feature): Boolean {
        return !feature.isPremium || billingManager.isPremium.value
    }

    val premiumFeatures: List<Feature>
        get() = Feature.entries.filter { it.isPremium }
}

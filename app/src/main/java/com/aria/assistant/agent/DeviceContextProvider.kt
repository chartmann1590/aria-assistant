package com.aria.assistant.agent

import com.aria.assistant.billing.BillingManager
import com.aria.assistant.permission.NotificationBridge
import com.aria.assistant.skill.CalendarSkill
import com.aria.assistant.skill.DeviceInfoSkill
import com.aria.assistant.skill.LocationSkill
import com.aria.assistant.skill.MediaSkill
import com.aria.assistant.skill.SkillResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceContext(
    val timeDate: String = "",
    val battery: String? = null,
    val wifi: String? = null,
    val location: String? = null,
    val upcomingEvents: String? = null,
    val nowPlaying: String? = null,
    val isPremium: Boolean = false,
    val notificationCount: Int? = null
) {
    fun toPromptSection(): String {
        val parts = mutableListOf<String>()
        if (timeDate.isNotBlank()) parts.add(timeDate)
        battery?.let { parts.add(it) }
        wifi?.let { parts.add(it) }
        location?.let { parts.add("Location: $it") }
        upcomingEvents?.let { parts.add("Events: $it") }
        nowPlaying?.let { parts.add("Now playing: $it") }
        val premiumLabel = if (isPremium) "Premium" else "Free"
        parts.add("Plan: $premiumLabel")
        notificationCount?.let { if (it > 0) parts.add("$it notification(s) active") }
        return parts.joinToString("; ").ifBlank { "" }
    }
}

@Singleton
class DeviceContextProvider @Inject constructor(
    private val deviceInfoSkill: DeviceInfoSkill,
    private val locationSkill: LocationSkill,
    private val calendarSkill: CalendarSkill,
    private val mediaSkill: MediaSkill,
    private val billingManager: BillingManager,
    private val notificationBridge: NotificationBridge
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cached: DeviceContext? = null
    private var lastCacheMs = 0L
    private val cacheTtlMs = 60_000L

    suspend fun snapshot(): DeviceContext {
        val now = System.currentTimeMillis()
        if (cached != null && (now - lastCacheMs) < cacheTtlMs) {
            return cached!!
        }
        val ctx = buildSnapshot()
        cached = ctx
        lastCacheMs = now
        return ctx
    }

    private suspend fun buildSnapshot(): DeviceContext = coroutineScope {
        val timeResult = deviceInfoSkill.time()
        val timeDate = (timeResult as? SkillResult.Success)?.data ?: ""

        val battery = try {
            val r = deviceInfoSkill.battery()
            (r as? SkillResult.Success)?.data
        } catch (_: SecurityException) { null }

        val wifi = try {
            val r = deviceInfoSkill.wifiState()
            (r as? SkillResult.Success)?.data
        } catch (_: SecurityException) { null }

        val location = async {
            try {
                withTimeoutOrNull(1_500L) {
                    val loc = locationSkill.currentLocation()
                    (loc as? SkillResult.Success)?.data
                }
            } catch (_: CancellationException) { null }
              catch (_: SecurityException) { null }
        }

        val events = async {
            try {
                withTimeoutOrNull(1_000L) {
                    val ev = calendarSkill.listEvents(null, null)
                    (ev as? SkillResult.Success)?.data
                }
            } catch (_: CancellationException) { null }
              catch (_: SecurityException) { null }
        }

        val nowPlaying = try {
            val res = mediaSkill.nowPlaying()
            (res as? SkillResult.Success)?.data
        } catch (_: SecurityException) { null }

        val isPremium = billingManager.isPremium.value

        val notifCount = try {
            notificationBridge.getActive(null).size
        } catch (_: Exception) { null }

        DeviceContext(
            timeDate = timeDate,
            battery = battery,
            wifi = wifi,
            location = location.await(),
            upcomingEvents = events.await(),
            nowPlaying = nowPlaying,
            isPremium = isPremium,
            notificationCount = notifCount
        )
    }
}

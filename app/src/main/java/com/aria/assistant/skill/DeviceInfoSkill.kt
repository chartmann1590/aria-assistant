package com.aria.assistant.skill

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class DeviceInfoSkill @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun battery(): SkillResult<String> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent == null) return SkillResult.Failure("Could not read battery info")
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return if (pct >= 0) {
            val chargeText = if (charging) " and charging" else ""
            SkillResult.Success("Battery at $pct%$chargeText")
        } else {
            SkillResult.Failure("Could not read battery level")
        }
    }

    fun time(): SkillResult<String> {
        val now = System.currentTimeMillis()
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))
        val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now))
        return SkillResult.Success("$date, $time")
    }

    fun wifiState(): SkillResult<String> {
        val wm = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return SkillResult.Success("Wi-Fi info unavailable")
        return try {
            if (wm.isWifiEnabled) {
                val info = wm.connectionInfo
                if (info != null && !info.ssid.isNullOrBlank() && info.ssid != "<unknown ssid>") {
                    SkillResult.Success("Wi-Fi connected to ${info.ssid}")
                } else {
                    SkillResult.Success("Wi-Fi is on but not connected")
                }
            } else {
                SkillResult.Success("Wi-Fi is off")
            }
        } catch (e: SecurityException) {
            SkillResult.Success("Wi-Fi info unavailable")
        }
    }

    fun storage(): SkillResult<String> {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.freeBytes
        val totalGb = totalBytes.toFloat() / (1024 * 1024 * 1024)
        val freeGb = freeBytes.toFloat() / (1024 * 1024 * 1024)
        val usedPct = ((totalBytes - freeBytes).toFloat() / totalBytes * 100).toInt()
        return SkillResult.Success("Storage: ${freeGb.toInt()} GB free of ${totalGb.toInt()} GB total ($usedPct% used)")
    }

    fun deviceContext(): String {
        val batteryResult = battery()
        val timeResult = time()
        val wifiResult = wifiState()
        val parts = mutableListOf<String>()
        if (batteryResult is SkillResult.Success) parts.add(batteryResult.data)
        if (timeResult is SkillResult.Success) parts.add(timeResult.data)
        if (wifiResult is SkillResult.Success) parts.add(wifiResult.data)
        return if (parts.isEmpty()) "" else parts.joinToString("; ")
    }
}

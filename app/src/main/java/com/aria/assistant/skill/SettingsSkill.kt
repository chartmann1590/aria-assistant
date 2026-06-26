package com.aria.assistant.skill

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.permission.PermissionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsSkill @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {
    fun changeSetting(key: String, value: String): SkillResult<String> {
        return when (key) {
            "brightness" -> adjustBrightness(value)
            "volume" -> adjustVolume(value)
            "wifi" -> setWifi(value)
            "bluetooth" -> setBluetooth(value)
            "dnd", "do_not_disturb" -> setDnd(value)
            "airplane", "airplane_mode" -> openPanel(key, value)
            "hotspot" -> openPanel(key, value)
            "battery_saver", "battery saver" -> openPanel(key, value)
            else -> SkillResult.Failure("Unknown setting: $key")
        }
    }

    private fun adjustBrightness(value: String): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.WRITE_SETTINGS)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.WRITE_SETTINGS)
        }
        val level = when {
            value.equals("up", ignoreCase = true) || value.equals("on", ignoreCase = true) -> {
                val current = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, 128
                )
                (current + 30).coerceIn(0, 255)
            }
            value.equals("down", ignoreCase = true) || value.equals("off", ignoreCase = true) -> {
                val current = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, 128
                )
                (current - 30).coerceIn(0, 255)
            }
            else -> {
                val numeric = value.toIntOrNull()
                if (numeric != null) {
                    (numeric * 255 / 100).coerceIn(0, 255)
                } else {
                    return SkillResult.Failure("I didn't understand that brightness value")
                }
            }
        }
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS, level
        )
        return SkillResult.Success("Brightness adjusted")
    }

    private fun adjustVolume(direction: String): SkillResult<String> {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val flag = if (direction == "up") AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, flag, AudioManager.FLAG_SHOW_UI)
        return SkillResult.Success("Volume turned $direction")
    }

    private fun setWifi(value: String): SkillResult<String> {
        val enable = value.equals("on", ignoreCase = true) ||
            value.equals("enable", ignoreCase = true) ||
            value.equals("true", ignoreCase = true)
        if (enable) {
            val panel = Intent(Settings.Panel.ACTION_WIFI).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(panel)
            return SkillResult.Success("Opening Wi-Fi settings")
        }
        return SkillResult.Failure("I can't turn Wi-Fi off directly")
    }

    @Suppress("DEPRECATION")
    private fun setBluetooth(value: String): SkillResult<String> {
        val enable = value.equals("on", ignoreCase = true) ||
            value.equals("enable", ignoreCase = true) ||
            value.equals("true", ignoreCase = true)
        val perm = permissionManager.ensure(PhoneCapability.BLUETOOTH)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.BLUETOOTH)
        }
        val bt = BluetoothAdapter.getDefaultAdapter()
        if (bt == null) {
            return SkillResult.Failure("This device doesn't have Bluetooth")
        }
        if (enable) bt.enable() else bt.disable()
        return SkillResult.Success(if (enable) "Bluetooth turned on" else "Bluetooth turned off")
    }

    private fun setDnd(value: String): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.DND)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.DND)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val enable = value.equals("on", ignoreCase = true) ||
            value.equals("enable", ignoreCase = true) ||
            value.equals("true", ignoreCase = true)
        if (enable) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            return SkillResult.Success("Do Not Disturb turned on")
        } else {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            return SkillResult.Success("Do Not Disturb turned off")
        }
    }

    private fun openPanel(key: String, value: String): SkillResult<String> {
        val enable = value.equals("on", ignoreCase = true) ||
            value.equals("enable", ignoreCase = true) ||
            value.equals("true", ignoreCase = true)
        val intent: Intent = when (key) {
            "airplane", "airplane_mode" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            "hotspot" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "battery_saver", "battery saver" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            else -> return SkillResult.Failure("I can't control $key directly")
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        return SkillResult.Success(if (enable) "Opening $key settings to turn on" else "Opening $key settings")
    }
}

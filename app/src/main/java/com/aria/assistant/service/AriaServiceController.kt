package com.aria.assistant.service

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AriaServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun startService() {
        val intent = Intent(context, AriaForegroundService::class.java)
        context.startForegroundService(intent)
    }

    fun stopService() {
        val intent = Intent(context, AriaForegroundService::class.java)
        context.stopService(intent)
    }

    fun muteService() {
        val intent = Intent(context, AriaForegroundService::class.java).apply {
            action = AriaForegroundService.ACTION_MUTE
        }
        context.startService(intent)
    }

    fun triggerListening() {
        val intent = Intent(context, AriaForegroundService::class.java).apply {
            action = AriaForegroundService.ACTION_TRIGGER
        }
        context.startService(intent)
    }

    fun cancelListening() {
        val intent = Intent(context, AriaForegroundService::class.java).apply {
            action = AriaForegroundService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    fun needsBatteryOptimization(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun createBatteryOptIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}

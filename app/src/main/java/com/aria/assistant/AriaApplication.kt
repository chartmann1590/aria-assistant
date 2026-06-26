package com.aria.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.aria.assistant.BuildConfig
import com.aria.assistant.engine.AriaLogger
import com.aria.assistant.service.AriaForegroundService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AriaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AriaLogger.d("AriaApplication", "App starting, debug mode: ${BuildConfig.DEBUG_MODE}")
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            AriaForegroundService.CHANNEL_ID,
            "Aria Voice Assistant",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows Aria's current status"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}

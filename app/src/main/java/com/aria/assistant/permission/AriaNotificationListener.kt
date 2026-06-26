package com.aria.assistant.permission

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aria.assistant.engine.AriaLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AriaNotificationListener : NotificationListenerService() {

    @Inject lateinit var bridge: NotificationBridge

    override fun onListenerConnected() {
        AriaLogger.d("AriaNotificationListener", "Notification listener connected")
        bridge.bind(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        bridge.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        bridge.onNotificationRemoved(sbn)
    }

    override fun onListenerDisconnected() {
        AriaLogger.d("AriaNotificationListener", "Notification listener disconnected")
        bridge.unbind()
    }

    override fun onDestroy() {
        super.onDestroy()
        bridge.unbind()
    }
}

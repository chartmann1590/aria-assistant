package com.aria.assistant.permission

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aria.assistant.engine.AriaLogger
import com.aria.assistant.skill.SkillResult
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class NotifSnapshot(
    val key: String,
    val packageName: String,
    val tag: String?,
    val id: Int,
    val title: String,
    val text: String,
    val isClearable: Boolean,
    val notification: Notification?
)

@Singleton
class NotificationBridge @Inject constructor() {

    private var listenerRef: WeakReference<NotificationListenerService> = WeakReference(null)
    private val activeNotifications = ConcurrentHashMap<String, NotifSnapshot>()

    fun isBound(): Boolean = listenerRef.get() != null

    fun bind(service: NotificationListenerService) {
        listenerRef = WeakReference(service)
        AriaLogger.d("NotificationBridge", "Bound to notification listener")
        refresh()
    }

    fun unbind() {
        listenerRef.clear()
        activeNotifications.clear()
        AriaLogger.d("NotificationBridge", "Unbound from notification listener")
    }

    fun refresh() {
        activeNotifications.clear()
        val listener = listenerRef.get() ?: return
        try {
            val sbns = listener.activeNotifications
            for (sbn in sbns) {
                addSnapshot(sbn)
            }
        } catch (e: SecurityException) {
            AriaLogger.e("NotificationBridge", "SecurityException refreshing notifications: ${e.message}")
        }
    }

    private fun addSnapshot(sbn: StatusBarNotification) {
        val title = sbn.notification.extras?.getString(Notification.EXTRA_TITLE)
            ?: sbn.notification.extras?.getString(Notification.EXTRA_SUB_TEXT) ?: ""
        val text = sbn.notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        activeNotifications[sbn.key] = NotifSnapshot(
            key = sbn.key,
            packageName = sbn.packageName,
            tag = sbn.tag,
            id = sbn.id,
            title = title,
            text = text,
            isClearable = sbn.isClearable,
            notification = sbn.notification
        )
    }

    fun onNotificationPosted(sbn: StatusBarNotification) {
        addSnapshot(sbn)
    }

    fun onNotificationRemoved(sbn: StatusBarNotification) {
        activeNotifications.remove(sbn.key)
    }

    fun getActive(filter: String? = null): List<NotifSnapshot> {
        return if (filter != null) {
            activeNotifications.values.filter {
                it.title.contains(filter, ignoreCase = true) ||
                it.text.contains(filter, ignoreCase = true) ||
                it.packageName.contains(filter, ignoreCase = true)
            }.toList()
        } else {
            activeNotifications.values.toList()
        }
    }

    fun reply(notificationKey: String, replyText: String): SkillResult<String> {
        val snapshot = activeNotifications[notificationKey]
            ?: return SkillResult.Failure("Notification no longer active")
        val notif = snapshot.notification
            ?: return SkillResult.Failure("Notification details unavailable")

        val replyAction = notif.actions?.firstOrNull { action ->
            action.remoteInputs?.isNotEmpty() == true
        } ?: return SkillResult.Failure("This notification does not support inline replies")

        val intent = Intent().apply {
            val results = Bundle()
            for (ri in replyAction.remoteInputs) {
                results.putCharSequence(ri.resultKey, replyText)
            }
            putExtra(RemoteInput.EXTRA_RESULTS_DATA, results)
        }
        try {
            replyAction.actionIntent?.send(null, 0, intent)
            return SkillResult.Success("Reply sent")
        } catch (e: PendingIntent.CanceledException) {
            return SkillResult.Failure("Failed to send reply: ${e.message}")
        }
    }

    fun dismiss(notificationKey: String): SkillResult<String> {
        val listener = listenerRef.get()
            ?: return SkillResult.Failure("Notification listener not connected")
        val snapshot = activeNotifications[notificationKey]
            ?: return SkillResult.Failure("Notification not found")
        try {
            listener.cancelNotification(snapshot.packageName, snapshot.tag, snapshot.id)
            activeNotifications.remove(notificationKey)
            return SkillResult.Success("Notification dismissed")
        } catch (e: Exception) {
            return SkillResult.Failure("Failed to dismiss: ${e.message}")
        }
    }
}

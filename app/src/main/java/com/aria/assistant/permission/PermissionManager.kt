package com.aria.assistant.permission

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.NotificationManager
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun status(cap: PhoneCapability): PermissionResult {
        val spec = specs[cap] ?: return PermissionResult.Granted
        return when (spec) {
            is CapabilitySpec.Runtime -> statusRuntime(spec)
            is CapabilitySpec.Special -> statusSpecial(spec)
        }
    }

    fun ensure(cap: PhoneCapability): PermissionResult = status(cap)

    fun runtimePerms(cap: PhoneCapability): List<String> {
        val spec = specs[cap] ?: return emptyList()
        return (spec as? CapabilitySpec.Runtime)?.perms ?: emptyList()
    }

    fun requestIntent(cap: PhoneCapability): Intent {
        val spec = specs[cap] ?: throw IllegalArgumentException("$cap is not registered")
        return (spec as? CapabilitySpec.Special)?.intentFactory?.invoke()
            ?: throw IllegalArgumentException("$cap is a runtime permission; use ActivityResult launcher")
    }

    private val specs: Map<PhoneCapability, CapabilitySpec> = mapOf(
        PhoneCapability.CALL to CapabilitySpec.Runtime(
            perms = listOf(android.Manifest.permission.CALL_PHONE)
        ),
        PhoneCapability.SMS to CapabilitySpec.Runtime(
            perms = listOf(android.Manifest.permission.SEND_SMS)
        ),
        PhoneCapability.READ_SMS to CapabilitySpec.Runtime(
            perms = listOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS)
        ),
        PhoneCapability.CONTACTS to CapabilitySpec.Runtime(
            perms = listOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS)
        ),
        PhoneCapability.WRITE_SETTINGS to CapabilitySpec.Special(
            check = { Settings.System.canWrite(context) },
            intentFactory = {
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        ),
        PhoneCapability.CALENDAR to CapabilitySpec.Runtime(
            perms = listOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR)
        ),
        PhoneCapability.NOTIFICATIONS to CapabilitySpec.Runtime(
            perms = listOf(android.Manifest.permission.POST_NOTIFICATIONS)
        ),
        PhoneCapability.NOTIFICATION_LISTENER to CapabilitySpec.Special(
            check = {
                val myPackage = context.packageName
                NotificationManagerCompat.getEnabledListenerPackages(context).contains(myPackage)
            },
            intentFactory = {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        ),
        PhoneCapability.ACCESSIBILITY to CapabilitySpec.Special(
            check = {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                val myId = "${context.packageName}/.permission.AriaAccessibilityService"
                am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                    .orEmpty().any { it.id == myId }
            },
            intentFactory = {
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        ),
        PhoneCapability.USAGE_ACCESS to CapabilitySpec.Special(
            check = {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                val mode = appOps?.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
                mode == android.app.AppOpsManager.MODE_ALLOWED
            },
            intentFactory = {
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        ),
        PhoneCapability.LOCATION to CapabilitySpec.Runtime(
            perms = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        ),
        PhoneCapability.CAMERA to CapabilitySpec.Runtime(
            perms = listOf(android.Manifest.permission.CAMERA)
        ),
        PhoneCapability.MEDIA_CONTROL to CapabilitySpec.Runtime(
            perms = emptyList()
        ),
        PhoneCapability.BLUETOOTH to CapabilitySpec.Runtime(
            perms = listOf(android.Manifest.permission.BLUETOOTH_CONNECT)
        ),
        PhoneCapability.APP_LAUNCH to CapabilitySpec.Special(
            check = { true },
            intentFactory = { throw UnsupportedOperationException("APP_LAUNCH is manifest-only") }
        ),
        PhoneCapability.BATTERY_OPT to CapabilitySpec.Special(
            check = {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
            },
            intentFactory = {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        ),
        PhoneCapability.DND to CapabilitySpec.Special(
            check = {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.isNotificationPolicyAccessGranted ?: false
            },
            intentFactory = {
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        )
    )

    private fun statusRuntime(spec: CapabilitySpec.Runtime): PermissionResult {
        val allGranted = spec.perms.all { perm ->
            if (perm.isEmpty()) return@all true
            context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
        }
        return if (allGranted) PermissionResult.Granted else PermissionResult.Denied(permanent = false)
    }

    private fun statusSpecial(spec: CapabilitySpec.Special): PermissionResult {
        return if (spec.check()) PermissionResult.Granted
        else PermissionResult.Denied(permanent = false)
    }

    private sealed interface CapabilitySpec {
        data class Runtime(val perms: List<String>) : CapabilitySpec
        data class Special(
            val check: () -> Boolean,
            val intentFactory: () -> Intent
        ) : CapabilitySpec
    }
}

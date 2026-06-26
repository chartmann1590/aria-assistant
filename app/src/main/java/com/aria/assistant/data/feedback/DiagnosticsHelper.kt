package com.aria.assistant.data.feedback

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DiagnosticsHelper {

    fun collect(context: Context): String {
        val pm = context.packageManager
        val packageName = context.packageName
        val appName = context.applicationInfo.loadLabel(pm).toString()

        val pInfo = try {
            pm.getPackageInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        val versionName = pInfo?.versionName ?: "?"
        val versionCode = if (Build.VERSION.SDK_INT >= 28) {
            pInfo?.longVersionCode?.toString()
        } else {
            pInfo?.versionCode?.toString()
        } ?: "?"

        val device = "${Build.BRAND} ${Build.MODEL}"
        val manufacturer = Build.MANUFACTURER
        val androidVersion = "${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}"
        val locale = Locale.getDefault().toString()
        val timeZone = TimeZone.getDefault().id
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val storageFree = try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            val bytes = stat.availableBlocksLong * stat.blockSizeLong
            formatBytes(bytes)
        } catch (_: Exception) { "?" }

        val storageTotal = try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            val bytes = stat.blockCountLong * stat.blockSizeLong
            formatBytes(bytes)
        } catch (_: Exception) { "?" }

        val memoryFree = try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            formatBytes(mi.availMem)
        } catch (_: Exception) { "?" }

        val memoryTotal = try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            formatBytes(mi.totalMem)
        } catch (_: Exception) { "?" }

        return buildString {
            appendLine("## Diagnostics")
            appendLine()
            appendLine("- Timestamp: $timestamp")
            appendLine("- App: $appName")
            appendLine("- Package: $packageName")
            appendLine("- Version: $versionName ($versionCode)")
            appendLine("- Device: $device")
            appendLine("- Manufacturer: $manufacturer")
            appendLine("- Android: $androidVersion")
            appendLine("- Locale: $locale")
            appendLine("- Time Zone: $timeZone")
            appendLine("- Storage Free/Total: $storageFree / $storageTotal")
            appendLine("- Memory Free/Total: $memoryFree / $memoryTotal")
        }
    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return String.format("%.1f %s", value, units[unitIndex])
    }
}

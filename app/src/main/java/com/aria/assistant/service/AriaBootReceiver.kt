package com.aria.assistant.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.aria.assistant.domain.repository.SettingsRepository
import com.aria.assistant.engine.AriaLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AriaBootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        // Android 14+ forbids starting a microphone foreground service from
        // BOOT_COMPLETED because RECORD_AUDIO is a while-in-use permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || !hasMicPermission) {
            AriaLogger.d("AriaBootReceiver", "Skipping microphone service start after boot")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bootStartEnabled = settingsRepository.isBootStartEnabled().first()
                if (shouldStartMicrophoneServiceAtBoot(
                        sdkInt = Build.VERSION.SDK_INT,
                        bootStartEnabled = bootStartEnabled,
                        hasMicPermission = hasMicPermission
                    )
                ) {
                    context.startForegroundService(Intent(context, AriaForegroundService::class.java))
                }
            } catch (error: RuntimeException) {
                AriaLogger.e("AriaBootReceiver", "Unable to start after boot", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

internal fun shouldStartMicrophoneServiceAtBoot(
    sdkInt: Int,
    bootStartEnabled: Boolean,
    hasMicPermission: Boolean
): Boolean = sdkInt < Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
    bootStartEnabled &&
    hasMicPermission

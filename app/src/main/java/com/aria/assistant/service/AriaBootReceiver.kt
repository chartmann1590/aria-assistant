package com.aria.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aria.assistant.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class AriaBootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val bootStartEnabled = runBlocking {
            settingsRepository.isBootStartEnabled().first()
        }

        if (bootStartEnabled) {
            val serviceIntent = Intent(context, AriaForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}

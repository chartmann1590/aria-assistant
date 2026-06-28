package com.aria.assistant.domain.repository

import com.aria.assistant.domain.model.VoiceConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getVoiceConfig(): Flow<VoiceConfig>
    suspend fun updateVoiceConfig(config: VoiceConfig)
    fun isOnboardingComplete(): Flow<Boolean>
    suspend fun setOnboardingComplete(complete: Boolean)
    fun isPremiumEnabled(): Flow<Boolean>
    suspend fun setPremiumEnabled(enabled: Boolean)
    fun isBootStartEnabled(): Flow<Boolean>
    suspend fun setBootStartEnabled(enabled: Boolean)
    suspend fun updatePrivacyMode(enabled: Boolean)
    suspend fun updateSelectedModel(model: String)
}

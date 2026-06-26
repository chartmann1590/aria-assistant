package com.aria.assistant.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aria.assistant.domain.model.VoiceConfig
import com.aria.assistant.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aria_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_WORD_SENSITIVITY = floatPreferencesKey("wake_word_sensitivity")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val LANGUAGE = stringPreferencesKey("language")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val PREMIUM_ENABLED = booleanPreferencesKey("premium_enabled")
        val SELECTED_VOICE = stringPreferencesKey("selected_voice")
        val BOOT_START = booleanPreferencesKey("boot_start")
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
    }

    override fun getVoiceConfig(): Flow<VoiceConfig> = context.dataStore.data.map { prefs ->
        VoiceConfig(
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: true,
            wakeWordSensitivity = prefs[Keys.WAKE_WORD_SENSITIVITY] ?: 0.5f,
            ttsSpeed = prefs[Keys.TTS_SPEED] ?: 1.0f,
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.0f,
            language = "en-US",
            selectedVoice = prefs[Keys.SELECTED_VOICE] ?: "en_US-amy-medium",
            temperatureUnit = prefs[Keys.TEMPERATURE_UNIT] ?: "celsius"
        )
    }

    override suspend fun updateVoiceConfig(config: VoiceConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WAKE_WORD_ENABLED] = config.wakeWordEnabled
            prefs[Keys.WAKE_WORD_SENSITIVITY] = config.wakeWordSensitivity
            prefs[Keys.TTS_SPEED] = config.ttsSpeed
            prefs[Keys.TTS_PITCH] = config.ttsPitch
            prefs[Keys.LANGUAGE] = config.language
            prefs[Keys.SELECTED_VOICE] = config.selectedVoice
            prefs[Keys.TEMPERATURE_UNIT] = config.temperatureUnit
        }
    }

    override fun isOnboardingComplete(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = complete
        }
    }

    override fun isPremiumEnabled(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PREMIUM_ENABLED] ?: false
    }

    override suspend fun setPremiumEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREMIUM_ENABLED] = enabled
        }
    }

    override fun isBootStartEnabled(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BOOT_START] ?: false
    }

    override suspend fun setBootStartEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BOOT_START] = enabled
        }
    }
}

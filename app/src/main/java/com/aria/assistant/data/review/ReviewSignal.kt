package com.aria.assistant.data.review

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aria_review_prefs")

/** Spoken responses before we ever ask for a review. Early asks convert worse. */
private const val RESPONSES_BEFORE_FIRST_ASK = 3

/**
 * Decides *when* to ask for a review (DataStore-backed, no Activity needed) and hands off to
 * whoever holds an Activity to actually launch Google's official In-App Review dialog. Google's
 * own quota caps how often the dialog can appear regardless of what we request, so this only
 * needs to avoid asking on someone's very first response and never ask twice.
 */
@Singleton
class ReviewSignal @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val RESPONSE_COUNT = intPreferencesKey("review_prompt_response_count")
        val REQUESTED = booleanPreferencesKey("review_prompt_requested")
    }

    private val _readyToPrompt = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val readyToPrompt: SharedFlow<Unit> = _readyToPrompt.asSharedFlow()

    /** Call after Aria successfully speaks a response — a genuine positive moment. */
    suspend fun recordSuccessfulResponse() {
        var shouldPrompt = false
        context.dataStore.edit { prefs ->
            val alreadyRequested = prefs[Keys.REQUESTED] ?: false
            val count = (prefs[Keys.RESPONSE_COUNT] ?: 0) + 1
            prefs[Keys.RESPONSE_COUNT] = count
            if (!alreadyRequested && count >= RESPONSES_BEFORE_FIRST_ASK) {
                prefs[Keys.REQUESTED] = true
                shouldPrompt = true
            }
        }
        if (shouldPrompt) _readyToPrompt.tryEmit(Unit)
    }
}

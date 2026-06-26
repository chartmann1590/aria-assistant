package com.aria.assistant.data.feedback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.feedbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feedback_bug_reports"
)

@Singleton
class BugReportRepo @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val BUG_REPORTS_LIST = stringPreferencesKey("bug_reports_list")
    }

    val bugReports: Flow<List<BugReport>> = context.feedbackDataStore.data.map { prefs ->
        val raw = prefs[Keys.BUG_REPORTS_LIST]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<BugReport>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveBugReport(report: BugReport) {
        context.feedbackDataStore.edit { prefs ->
            val raw = prefs[Keys.BUG_REPORTS_LIST]
            val list = if (raw.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<BugReport>>(raw)
                } catch (_: Exception) {
                    emptyList()
                }
            }.toMutableList()

            val existingIndex = list.indexOfFirst { it.number == report.number }
            if (existingIndex >= 0) {
                list[existingIndex] = report
            } else {
                list.add(report)
            }
            list.sortByDescending { it.number }
            prefs[Keys.BUG_REPORTS_LIST] = json.encodeToString(list)
        }
    }

    suspend fun updateBugReports(reports: List<BugReport>) {
        context.feedbackDataStore.edit { prefs ->
            val sorted = reports.sortedByDescending { it.number }
            prefs[Keys.BUG_REPORTS_LIST] = json.encodeToString(sorted)
        }
    }

    suspend fun getBugReportsList(): List<BugReport> {
        return context.feedbackDataStore.data.first().let { prefs ->
            val raw = prefs[Keys.BUG_REPORTS_LIST]
            if (raw.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<BugReport>>(raw)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
    }
}

package com.aria.assistant.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.aria.assistant.data.feedback.DiagnosticsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    val appVersion: String = try {
        val pm = context.packageManager
        val pInfo = pm.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "?"
    } catch (_: Exception) { "?" }

    val diagnostics: String = DiagnosticsHelper.collect(context)
}

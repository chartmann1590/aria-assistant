package com.aria.assistant.engine

import android.util.Log
import com.aria.assistant.BuildConfig

object AriaLogger {
    private const val TAG = "Aria"
    private val isDebug: Boolean get() = BuildConfig.DEBUG_MODE

    fun d(tag: String, message: String) {
        try { if (isDebug) Log.d(TAG, "[$tag] $message") } catch (_: RuntimeException) { }
    }

    fun i(tag: String, message: String) {
        try { if (isDebug) Log.i(TAG, "[$tag] $message") } catch (_: RuntimeException) { }
    }

    fun w(tag: String, message: String) {
        try { Log.w(TAG, "[$tag] $message") } catch (_: RuntimeException) { }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try { Log.e(TAG, "[$tag] $message", throwable) } catch (_: RuntimeException) { }
    }
}

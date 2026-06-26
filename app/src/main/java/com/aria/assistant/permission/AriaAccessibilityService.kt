package com.aria.assistant.permission

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.aria.assistant.engine.AriaLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AriaAccessibilityService : AccessibilityService() {

    @Inject lateinit var bridge: AccessibilityBridge

    override fun onServiceConnected() {
        super.onServiceConnected()
        AriaLogger.d("AriaAccessibilityService", "Accessibility service connected")
        bridge.bind(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
        AriaLogger.d("AriaAccessibilityService", "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        bridge.unbind()
    }
}

package com.aria.assistant.permission

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.aria.assistant.engine.AriaLogger
import com.aria.assistant.skill.SkillResult
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityBridge @Inject constructor() {

    private var serviceRef: WeakReference<AccessibilityService> = WeakReference(null)

    fun isBound(): Boolean = serviceRef.get() != null

    fun bind(service: AccessibilityService) {
        serviceRef = WeakReference(service)
        AriaLogger.d("AccessibilityBridge", "Bound to accessibility service")
    }

    fun unbind() {
        serviceRef.clear()
        AriaLogger.d("AccessibilityBridge", "Unbound from accessibility service")
    }

    fun readScreen(): SkillResult<String> {
        val service = serviceRef.get() ?: return SkillResult.Failure("Accessibility service not connected")
        val root = service.rootInActiveWindow ?: return SkillResult.Failure("No active window found")
        try {
            val text = collectText(root)
            return SkillResult.Success(text.ifBlank { "No readable content on screen" })
        } catch (e: Exception) {
            return SkillResult.Failure("Error reading screen: ${e.message}")
        } finally {
            root.recycle()
        }
    }

    fun findClickableLabels(): SkillResult<List<String>> {
        val service = serviceRef.get() ?: return SkillResult.Failure("Accessibility service not connected")
        val root = service.rootInActiveWindow ?: return SkillResult.Failure("No active window found")
        try {
            val labels = mutableListOf<String>()
            collectClickableLabels(root, labels)
            return SkillResult.Success(labels.distinct())
        } catch (e: Exception) {
            return SkillResult.Failure("Error finding labels: ${e.message}")
        } finally {
            root.recycle()
        }
    }

    fun clickLabel(label: String): SkillResult<String> {
        val service = serviceRef.get() ?: return SkillResult.Failure("Accessibility service not connected")
        val root = service.rootInActiveWindow ?: return SkillResult.Failure("No active window found")
        try {
            val node = findLabel(root, label) ?: return SkillResult.Failure("Could not find '$label' on screen")
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return SkillResult.Success("Clicked '$label'")
        } catch (e: Exception) {
            return SkillResult.Failure("Error clicking '$label': ${e.message}")
        } finally {
            root.recycle()
        }
    }

    fun scroll(direction: String): SkillResult<String> {
        val service = serviceRef.get() ?: return SkillResult.Failure("Accessibility service not connected")
        val root = service.rootInActiveWindow ?: return SkillResult.Failure("No active window found")
        try {
            val scrollable = findScrollable(root, direction) ?: return SkillResult.Failure("No scrollable area found")
            val action = when (direction.lowercase()) {
                "up", "upward" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                "down", "downward" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                "left" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                "right" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            scrollable.performAction(action)
            return SkillResult.Success("Scrolled $direction")
        } catch (e: Exception) {
            return SkillResult.Failure("Error scrolling: ${e.message}")
        } finally {
            root.recycle()
        }
    }

    private fun collectText(node: AccessibilityNodeInfo, depth: Int = 0): String {
        if (depth > 20) return ""
        val sb = StringBuilder()
        if (node.text != null && node.isVisibleToUser) {
            sb.append(node.text)
            if (node.contentDescription != null) {
                sb.append(" (").append(node.contentDescription).append(")")
            }
            sb.append(" ")
        } else if (node.contentDescription != null && node.isVisibleToUser) {
            sb.append(node.contentDescription).append(" ")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            sb.append(collectText(child, depth + 1))
            child.recycle()
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun collectClickableLabels(node: AccessibilityNodeInfo, labels: MutableList<String>) {
        if (node.isClickable && node.text != null && node.isVisibleToUser) {
            labels.add(node.text.toString())
        }
        if (node.isClickable && node.contentDescription != null && node.isVisibleToUser) {
            labels.add(node.contentDescription.toString())
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectClickableLabels(child, labels)
            child.recycle()
        }
    }

    private fun findLabel(node: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(label, ignoreCase = true) == true && node.isVisibleToUser && node.isClickable) {
            return node
        }
        if (node.contentDescription?.toString()?.contains(label, ignoreCase = true) == true && node.isVisibleToUser && node.isClickable) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findLabel(child, label)
            if (found != null) {
                child.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun findScrollable(node: AccessibilityNodeInfo, direction: String): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollable(child, direction)
            if (found != null) {
                child.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }
}

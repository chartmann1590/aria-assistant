package com.aria.assistant.skill

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardSkill @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun read(): SkillResult<String> {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?:
            return SkillResult.Failure("Clipboard not available")
        val clip = clipboard.primaryClip ?: return SkillResult.Success("Clipboard is empty")
        val item = clip.getItemAt(0) ?: return SkillResult.Success("Clipboard is empty")
        val text = item.text?.toString() ?: item.coerceToText(context)?.toString() ?: "Clipboard contains non-text content"
        return SkillResult.Success(text)
    }

    fun write(text: String): SkillResult<String> {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?:
            return SkillResult.Failure("Clipboard not available")
        clipboard.setPrimaryClip(ClipData.newPlainText("aria", text))
        return SkillResult.Success("Copied to clipboard")
    }
}

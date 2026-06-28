package com.aria.assistant.skill

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailSkill @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun compose(to: String? = null, subject: String? = null, body: String? = null): SkillResult<String> {
        val uriStr = "mailto:${to.orEmpty()}"
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(uriStr)).apply {
            if (!subject.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
            if (!body.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, body)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return SkillResult.Success("Opening email app")
        }
        return SkillResult.Failure("No email app available")
    }
}

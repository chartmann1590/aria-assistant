package com.aria.assistant.skill

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLaunchSkill @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun open(appName: String): SkillResult<String> {
        val pm = context.packageManager
        val queryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = pm.queryIntentActivities(queryIntent, 0)
        val matched = activities.firstOrNull { resolveInfo ->
            val label = resolveInfo.loadLabel(pm).toString()
            label.contains(appName, ignoreCase = true) ||
            resolveInfo.activityInfo.packageName.contains(appName, ignoreCase = true) ||
            resolveInfo.activityInfo.name.contains(appName, ignoreCase = true)
        }
        if (matched == null) {
            return SkillResult.Failure("Could not find an app named '$appName'")
        }
        val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launchIntent)
            return SkillResult.Success("Opening $appName")
        }
        return SkillResult.Failure("Could not launch $appName")
    }

    fun listApps(filter: String?): SkillResult<String> {
        val pm = context.packageManager
        val queryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = pm.queryIntentActivities(queryIntent, 0)
        val apps = activities.mapNotNull {
            val label = it.loadLabel(pm).toString()
            if (filter == null || label.contains(filter, ignoreCase = true)) label else null
        }.distinct().sorted()

        if (apps.isEmpty()) {
            return SkillResult.Success(
                if (filter != null) "No apps found matching '$filter'" else "No apps found"
            )
        }
        val result = apps.joinToString(", ")
        return SkillResult.Success(result)
    }
}

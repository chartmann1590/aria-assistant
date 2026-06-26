package com.aria.assistant.skill

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.permission.PermissionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraSkill @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {
    fun takePhoto(label: String?): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.CAMERA)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.CAMERA)
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            val name = label ?: "photo"
            return SkillResult.Success("Opening camera to take $name")
        }
        return SkillResult.Failure("No camera app available")
    }

    fun getLatestPhoto(count: Int?): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.CAMERA)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.CAMERA)
        }
        val limit = count ?: 1
        val projection = arrayOf(
            Images.Media.DISPLAY_NAME,
            Images.Media.DATE_TAKEN,
            Images.Media._ID
        )
        val cursor = context.contentResolver.query(
            Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${Images.Media.DATE_TAKEN} DESC"
        )
        return cursor?.use {
            if (it.count == 0) return@use SkillResult.Success("No photos found")
            val sb = StringBuilder()
            var taken = 0
            while (it.moveToNext() && taken < limit) {
                val name = it.getString(0) ?: "photo"
                sb.append(name).append(", ")
                taken++
            }
            SkillResult.Success("Latest photo${if (limit > 1) "s" else ""}: ${sb.toString().trimEnd(',', ' ')}")
        } ?: SkillResult.Failure("Could not read photo library")
    }
}

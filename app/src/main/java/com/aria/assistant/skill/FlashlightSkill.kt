package com.aria.assistant.skill

import android.content.Context
import android.hardware.camera2.CameraManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashlightSkill @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun setFlashlight(on: Boolean): SkillResult<String> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?:
            return SkillResult.Failure("Camera service not available")
        return try {
            val cameraId = manager.cameraIdList.firstOrNull() ?:
                return SkillResult.Failure("No camera found")
            manager.setTorchMode(cameraId, on)
            SkillResult.Success(if (on) "Flashlight turned on" else "Flashlight turned off")
        } catch (e: Exception) {
            SkillResult.Failure("Failed to toggle flashlight: ${e.message}")
        }
    }
}

package com.aria.assistant.skill

import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSkill @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun getMediaController(): MediaController? {
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return null
        return try {
            msm.getActiveSessions(null).firstOrNull()
        } catch (_: SecurityException) {
            null
        }
    }

    fun playPause(): SkillResult<String> {
        val controller = getMediaController()
        if (controller == null) {
            return sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
        val state = controller.playbackState
        if (state == null || state.state == android.media.session.PlaybackState.STATE_NONE) {
            controller.getTransportControls().play()
            return SkillResult.Success("Playing media")
        }
        return if (state.state == android.media.session.PlaybackState.STATE_PLAYING) {
            controller.getTransportControls().pause()
            SkillResult.Success("Media paused")
        } else {
            controller.getTransportControls().play()
            SkillResult.Success("Playing media")
        }
    }

    fun next(): SkillResult<String> {
        val controller = getMediaController()
        if (controller != null) {
            controller.getTransportControls().skipToNext()
            return SkillResult.Success("Skipping to next track")
        }
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun prev(): SkillResult<String> {
        val controller = getMediaController()
        if (controller != null) {
            controller.getTransportControls().skipToPrevious()
            return SkillResult.Success("Going to previous track")
        }
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun nowPlaying(): SkillResult<String> {
        val controller = getMediaController()
        if (controller == null) return SkillResult.Failure("No media is currently playing")
        val metadata = controller.metadata
        if (metadata == null) return SkillResult.Failure("No media information available")
        val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        return if (artist.isNotBlank()) {
            SkillResult.Success("Now playing: $title by $artist")
        } else {
            SkillResult.Success("Now playing: $title")
        }
    }

    private fun sendMediaKey(keyCode: Int): SkillResult<String> {
        val controller = getMediaController()
        if (controller != null) {
            val event = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            controller.dispatchMediaButtonEvent(event)
            return SkillResult.Success("Media command sent")
        }
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        }
        context.sendBroadcast(intent)
        return SkillResult.Success("Media command sent")
    }
}

package com.aria.assistant.service

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AriaBootReceiverPolicyTest {

    @Test
    fun `modern Android never starts microphone service from boot receiver`() {
        assertFalse(
            shouldStartMicrophoneServiceAtBoot(
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                bootStartEnabled = true,
                hasMicPermission = true
            )
        )
    }

    @Test
    fun `legacy Android requires opt in and microphone permission`() {
        val legacySdk = Build.VERSION_CODES.TIRAMISU

        assertTrue(shouldStartMicrophoneServiceAtBoot(legacySdk, true, true))
        assertFalse(shouldStartMicrophoneServiceAtBoot(legacySdk, false, true))
        assertFalse(shouldStartMicrophoneServiceAtBoot(legacySdk, true, false))
    }
}

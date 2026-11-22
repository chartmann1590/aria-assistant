package com.aria.assistant.billing

import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayReviewAccessTest {
    private val fixture = "JBSWY3DPEHPK3PXP".repeat(2)
    private val verifier = MessageDigest.getInstance("SHA-256").digest(fixture.toByteArray(Charsets.US_ASCII))
        .joinToString("") { "%02x".format(it) }

    @Test fun validCodeIsReusableButOnlyActivatesItsOwnProcessSession() {
        val access = PlayReviewAccess(verifier)
        assertTrue(access.isConfigured)
        assertFalse(access.active.value)
        assertTrue(access.enter("  ${fixture.lowercase()}  "))
        assertTrue(access.active.value)
        assertFalse(PlayReviewAccess(verifier).active.value)
        access.end()
        assertFalse(access.active.value)
        assertTrue(access.enter(fixture))
    }

    @Test fun wrongMalformedOrDisabledCodesDoNotUnlock() {
        val access = PlayReviewAccess(verifier)
        assertFalse(access.enter("Y" + fixture.drop(1)))
        assertFalse(access.enter("9".repeat(32)))
        assertFalse(access.enter(""))
        assertFalse(access.enter("Y".repeat(45)))
        assertFalse(access.active.value)
        val disabled = PlayReviewAccess("")
        assertFalse(disabled.isConfigured)
        assertFalse(disabled.enter(fixture))
        assertFalse(disabled.active.value)
    }
}

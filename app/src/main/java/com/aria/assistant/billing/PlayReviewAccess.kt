package com.aria.assistant.billing

import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local release-review access. It never writes, restores, or acknowledges a purchase. */
internal class PlayReviewAccess(private val verifier: String) {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()
    val isConfigured: Boolean = verifier.matches(Regex("[0-9a-f]{64}"))

    fun enter(input: String): Boolean {
        if (!isConfigured || input.length > 44) return false
        val code = input.trim().uppercase(Locale.ROOT)
        if (!code.matches(Regex("[A-Z2-7]{32}"))) return false
        val hashed = MessageDigest.getInstance("SHA-256").digest(code.toByteArray(Charsets.US_ASCII))
        val expected = ByteArray(32) { index -> verifier.substring(2 * index, 2 * index + 2).toInt(16).toByte() }
        if (!MessageDigest.isEqual(hashed, expected)) return false
        _active.value = true
        return true
    }

    fun end() { _active.value = false }
}

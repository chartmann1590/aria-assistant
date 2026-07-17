package com.aria.assistant.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class PublicUrlValidatorTest {
    private val validator = PublicUrlValidator()

    @Test
    fun `only public shaped https urls are accepted`() {
        assertTrue(validator.hasAllowedShape("https://example.com/article"))
        assertFalse(validator.hasAllowedShape("http://example.com"))
        assertFalse(validator.hasAllowedShape("file:///etc/passwd"))
        assertFalse(validator.hasAllowedShape("https://localhost/private"))
        assertFalse(validator.hasAllowedShape("https://user@example.com/"))
    }

    @Test
    fun `private and loopback addresses are rejected`() {
        assertFalse(validator.isPublicAddress(InetAddress.getByName("127.0.0.1")))
        assertFalse(validator.isPublicAddress(InetAddress.getByName("10.0.0.1")))
        assertFalse(validator.isPublicAddress(InetAddress.getByName("192.168.1.1")))
        assertFalse(validator.isPublicAddress(InetAddress.getByName("::1")))
        assertTrue(validator.isPublicAddress(InetAddress.getByName("8.8.8.8")))
    }
}

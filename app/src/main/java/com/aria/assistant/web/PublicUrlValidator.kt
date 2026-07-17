package com.aria.assistant.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

class PublicUrlValidator {

    suspend fun isAllowed(rawUrl: String): Boolean = withContext(Dispatchers.IO) {
        val uri = parsePublicHttps(rawUrl) ?: return@withContext false
        runCatching {
            InetAddress.getAllByName(uri.host).isNotEmpty() &&
                InetAddress.getAllByName(uri.host).all(::isPublicAddress)
        }.getOrDefault(false)
    }

    fun hasAllowedShape(rawUrl: String): Boolean = parsePublicHttps(rawUrl) != null

    private fun parsePublicHttps(rawUrl: String): URI? = runCatching {
        val uri = URI(rawUrl.trim())
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.host.isNullOrBlank() || uri.userInfo != null) return null
        if (isBlockedHost(uri.host)) return null
        uri
    }.getOrNull()

    private fun isBlockedHost(host: String): Boolean {
        val normalized = host.trimEnd('.').lowercase()
        return normalized == "localhost" ||
            normalized.endsWith(".localhost") ||
            normalized.endsWith(".local") ||
            normalized.endsWith(".internal") ||
            normalized == "0.0.0.0"
    }

    internal fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress) return false

        val bytes = address.address
        if (address is Inet4Address && bytes.size == 4) {
            val a = bytes[0].toInt() and 0xff
            val b = bytes[1].toInt() and 0xff
            return when {
                a == 0 || a == 10 || a == 127 -> false
                a == 100 && b in 64..127 -> false
                a == 169 && b == 254 -> false
                a == 172 && b in 16..31 -> false
                a == 192 && b == 168 -> false
                a == 192 && b == 0 -> false
                a == 198 && b in 18..19 -> false
                a >= 224 -> false
                else -> true
            }
        }
        if (address is Inet6Address) {
            val first = bytes[0].toInt() and 0xff
            if ((first and 0xfe) == 0xfc) return false // unique-local fc00::/7
            if (bytes.all { it.toInt() == 0 }) return false
        }
        return true
    }
}

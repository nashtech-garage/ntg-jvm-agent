package com.ntgjvmagent.orchestrator.ingestion.utils

import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException

@Component
class UrlSafetyValidator {
    companion object {
        const val UNSIGNED_BYTE_MASK = 0xFF

        val ALLOWED_SCHEMES = setOf("http", "https")

        private const val PRIVATE_A = 10
        private const val PRIVATE_B = 172
        private val PRIVATE_B_SECOND_RANGE = 16..31
        private const val PRIVATE_C = 192
        private const val PRIVATE_C_SECOND = 168
    }

    fun validate(rawUrl: String) {
        val uri = parseUrl(rawUrl)
        validateScheme(uri)
        val host = extractHost(uri)
        validateAddresses(host)
    }

    private fun parseUrl(raw: String): URI =
        try {
            URI(raw).normalize()
        } catch (ex: IllegalArgumentException) {
            throw WebPageFetchException("Invalid URL: $raw", ex)
        }

    private fun validateScheme(uri: URI) {
        val scheme = uri.scheme ?: throw WebPageFetchException("Missing URL scheme: $uri")
        if (scheme !in ALLOWED_SCHEMES) {
            throw WebPageFetchException("Only ${ALLOWED_SCHEMES.joinToString()} allowed: $uri")
        }
    }

    private fun extractHost(uri: URI): String = uri.host ?: throw WebPageFetchException("Missing URL host: $uri")

    private fun validateAddresses(host: String) {
        val addresses =
            try {
                InetAddress.getAllByName(host)
            } catch (ex: UnknownHostException) {
                throw WebPageFetchException("Cannot resolve host: $host", ex)
            } catch (ex: SecurityException) {
                throw WebPageFetchException("Security manager blocked host lookup: $host", ex)
            }

        addresses.forEach { addr ->
            if (isLocal(addr) || isPrivate(addr)) {
                throw WebPageFetchException("Blocked private/internal address: $host ($addr)")
            }
        }
    }

    private fun isLocal(addr: InetAddress): Boolean =
        addr.isAnyLocalAddress ||
            addr.isLoopbackAddress ||
            addr.isLinkLocalAddress ||
            addr.isSiteLocalAddress

    private fun isPrivate(addr: InetAddress): Boolean {
        val octets = addr.address ?: return false
        val first = octets[0].toInt() and UNSIGNED_BYTE_MASK
        val second = octets[1].toInt() and UNSIGNED_BYTE_MASK

        return when (first) {
            PRIVATE_A -> true
            PRIVATE_B -> second in PRIVATE_B_SECOND_RANGE
            PRIVATE_C -> second == PRIVATE_C_SECOND
            else -> false
        }
    }
}

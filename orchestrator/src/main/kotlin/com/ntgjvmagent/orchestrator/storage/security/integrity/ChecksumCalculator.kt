package com.ntgjvmagent.orchestrator.storage.security.integrity

import org.springframework.stereotype.Component
import java.io.InputStream
import java.security.MessageDigest

@Component
class ChecksumCalculator {
    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024 // 8 KB
    }

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        input.use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

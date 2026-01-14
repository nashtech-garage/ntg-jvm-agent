package com.ntgjvmagent.orchestrator.storage.facade

import com.ntgjvmagent.orchestrator.storage.core.ObjectStorage
import com.ntgjvmagent.orchestrator.storage.security.integrity.ChecksumCalculator
import com.ntgjvmagent.orchestrator.storage.security.malware.VirusScanner
import com.ntgjvmagent.orchestrator.storage.security.resource.FileSizeValidator
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Component
class SecureObjectStorage(
    private val delegate: ObjectStorage,
    private val sizeValidator: FileSizeValidator,
    private val checksumCalculator: ChecksumCalculator,
    private val virusScanner: VirusScanner,
) {
    fun store(
        storageKey: String,
        file: MultipartFile,
    ): StoredFileMetadata {
        sizeValidator.validate(file.size)

        // 1️ Read once into temp file
        val temp = Files.createTempFile("upload-", ".bin")

        try {
            file.inputStream.use { input ->
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING)
            }

            // 2️ checksum
            val checksum =
                Files.newInputStream(temp).use {
                    checksumCalculator.sha256(it)
                }

            // 3️ virus scan
            Files.newInputStream(temp).use {
                virusScanner.scan(it)
            }

            // 4️ atomic store
            Files.newInputStream(temp).use {
                delegate.store(storageKey, it, file.contentType)
            }

            return StoredFileMetadata(
                checksumSha256 = checksum,
                fileSizeBytes = file.size,
            )
        } finally {
            Files.deleteIfExists(temp)
        }
    }
}

data class StoredFileMetadata(
    val checksumSha256: String,
    val fileSizeBytes: Long,
)

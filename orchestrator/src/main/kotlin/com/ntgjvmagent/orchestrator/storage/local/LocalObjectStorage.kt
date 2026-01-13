package com.ntgjvmagent.orchestrator.storage.local

import com.ntgjvmagent.orchestrator.storage.core.ObjectStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Component
@ConditionalOnProperty(
    name = ["storage.type"],
    havingValue = "local",
    matchIfMissing = true,
)
class LocalObjectStorage(
    @Value($$"${storage.local.base-path}")
    private val basePath: Path,
) : ObjectStorage {
    override fun store(
        storageKey: String,
        inputStream: InputStream,
        contentType: String?,
    ) {
        val finalPath = resolve(storageKey)
        val tmpPath = resolveTemp(storageKey)

        Files.createDirectories(finalPath.parent)
        Files.createDirectories(tmpPath.parent)

        try {
            // 1️ Write to temp file
            inputStream.use { input ->
                Files.copy(input, tmpPath, StandardCopyOption.REPLACE_EXISTING)
            }

            // 2️ Atomic move into place
            Files.move(
                tmpPath,
                finalPath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (ex: IOException) {
            // 3️ Clean up partial file
            Files.deleteIfExists(tmpPath)
            throw ex
        }
    }

    override fun load(storageKey: String): InputStream = Files.newInputStream(resolve(storageKey))

    override fun exists(storageKey: String): Boolean = Files.exists(resolve(storageKey))

    override fun delete(storageKey: String) {
        Files.deleteIfExists(resolve(storageKey))
        Files.deleteIfExists(resolveTemp(storageKey))
    }

    private fun resolve(storageKey: String): Path = basePath.resolve(storageKey)

    private fun resolveTemp(storageKey: String): Path = basePath.resolve("tmp").resolve("$storageKey.part")
}

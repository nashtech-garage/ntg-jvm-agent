package com.ntgjvmagent.orchestrator.storage.local

import com.ntgjvmagent.orchestrator.storage.core.ObjectStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
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
    private companion object {
        // Only allow safe, portable path characters
        private val STORAGE_KEY_REGEX = Regex("^[a-zA-Z0-9/_\\-.]+$")
    }

    override fun store(
        storageKey: String,
        inputStream: InputStream,
        contentType: String?,
    ) {
        validateStorageKey(storageKey)

        val finalPath = resolve(storageKey)
        val tmpPath = resolveTemp(storageKey)

        Files.createDirectories(finalPath.parent)
        Files.createDirectories(tmpPath.parent)

        try {
            // 1) Write to temp file
            inputStream.use { input ->
                Files.copy(input, tmpPath, StandardCopyOption.REPLACE_EXISTING)
            }

            // 2) Atomic move into place
            try {
                Files.move(
                    tmpPath,
                    finalPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                // Fallback: non-atomic but safe copy+replace
                Files.copy(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING)
                Files.delete(tmpPath)
            }
        } catch (ex: IOException) {
            // 3) Best-effort cleanup
            try {
                Files.deleteIfExists(tmpPath)
            } catch (_: Exception) {
                // ignored, best-effort
            }
            throw ex
        }
    }

    override fun load(storageKey: String): InputStream {
        validateStorageKey(storageKey)
        return Files.newInputStream(resolve(storageKey))
    }

    override fun exists(storageKey: String): Boolean {
        validateStorageKey(storageKey)
        return Files.exists(resolve(storageKey))
    }

    override fun delete(storageKey: String) {
        validateStorageKey(storageKey)
        Files.deleteIfExists(resolve(storageKey))
        Files.deleteIfExists(resolveTemp(storageKey))
    }

    // -------------------------------
    // Validation & sandboxing
    // -------------------------------

    private fun validateStorageKey(key: String) {
        require(key.isNotBlank()) { "storageKey must not be blank" }

        require(STORAGE_KEY_REGEX.matches(key)) {
            "storageKey contains illegal characters: $key"
        }

        require(!key.contains("..")) {
            "storageKey must not contain '..'"
        }

        require(!key.startsWith("/")) {
            "storageKey must be a relative path"
        }

        require(!key.startsWith("\\")) {
            "storageKey must be a relative path"
        }
    }

    private fun resolve(storageKey: String): Path {
        val resolved = basePath.resolve(storageKey).normalize()
        ensureInsideBase(resolved)
        return resolved
    }

    private fun resolveTemp(storageKey: String): Path {
        val resolved =
            basePath
                .resolve("tmp")
                .resolve("$storageKey.part")
                .normalize()

        ensureInsideBase(resolved)
        return resolved
    }

    private fun ensureInsideBase(path: Path) {
        if (!path.startsWith(basePath)) {
            throw SecurityException("Resolved path escapes basePath: $path")
        }
    }
}

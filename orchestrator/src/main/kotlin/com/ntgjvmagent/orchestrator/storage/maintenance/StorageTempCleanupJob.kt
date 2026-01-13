package com.ntgjvmagent.orchestrator.storage.maintenance

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class StorageTempCleanupJob(
    @Value($$"${storage.local.base-path}")
    private val basePath: Path,
) {
    @Scheduled(cron = "0 0 * * * *") // every hour
    fun cleanup() {
        val tmpDir = basePath.resolve("tmp")

        if (!Files.exists(tmpDir)) return

        Files
            .walk(tmpDir)
            .filter { Files.isRegularFile(it) }
            .filter { it.fileName.toString().endsWith(".part") }
            .filter {
                Files
                    .getLastModifiedTime(it)
                    .toInstant()
                    .isBefore(Instant.now().minus(1, ChronoUnit.HOURS))
            }.forEach {
                try {
                    Files.deleteIfExists(it)
                } catch (_: Exception) {
                    // Best effort; log if you want
                }
            }
    }
}

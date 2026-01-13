package com.ntgjvmagent.orchestrator.storage.maintenance

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class TempFileCleanupJob {
    @Scheduled(cron = "0 0 * * * *") // every hour
    fun cleanup() {
        val tmpDir = Paths.get(System.getProperty("java.io.tmpdir"))

        Files
            .list(tmpDir)
            .filter { it.fileName.toString().startsWith("upload-") }
            .filter { Files.getLastModifiedTime(it).toInstant().isBefore(Instant.now().minus(1, ChronoUnit.HOURS)) }
            .forEach {
                try {
                    Files.deleteIfExists(it)
                } catch (_: Exception) {
                    // Best-effort cleanup. Ignored on purpose.
                }
            }
    }
}

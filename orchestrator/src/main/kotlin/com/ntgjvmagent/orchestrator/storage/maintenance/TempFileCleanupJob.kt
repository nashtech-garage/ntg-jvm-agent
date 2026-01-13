package com.ntgjvmagent.orchestrator.storage.maintenance

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class TempFileCleanupJob {
    private val logger = LoggerFactory.getLogger(javaClass)

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
                } catch (ex: IOException) {
                    logger.debug("Failed to delete temp file {}", it, ex)
                }
            }
    }
}

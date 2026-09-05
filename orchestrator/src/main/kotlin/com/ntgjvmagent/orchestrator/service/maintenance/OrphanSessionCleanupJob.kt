package com.ntgjvmagent.orchestrator.service.maintenance

import com.ntgjvmagent.orchestrator.config.SessionCleanupProperties
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.session.SessionService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Deletes sessions that no conversation points at.
 *
 * ConversationSessionService creates the session before the stream starts and the conversation
 * row is only written once the stream completes, so every failed or abandoned first message
 * leaves a session -- with its events -- behind. Nothing ever collected those: expires_at is
 * never set, so the library's own expiry sweep does not see them either.
 *
 * Sessions belonging to a deleted conversation are removed by ConversationCommandService at
 * delete time; this job is for the ones that never got a conversation at all.
 */
@Component
@ConditionalOnProperty(value = ["session.cleanup.enabled"], havingValue = "true", matchIfMissing = true)
class OrphanSessionCleanupJob(
    private val conversationRepository: ConversationRepository,
    private val sessionService: SessionService,
    private val properties: SessionCleanupProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${session.cleanup.cron}", zone = "UTC")
    fun cleanup(): Int {
        // A session younger than the grace period may belong to a stream that is still running,
        // or to one whose conversation is about to be written.
        val cutoff = LocalDateTime.ofInstant(Instant.now().minus(properties.orphanGrace), ZoneId.systemDefault())
        var deleted = 0
        var batches = 0

        // Batched and capped: the first run after this ships has every orphan ever created to
        // get through, and each delete cascades into that session's events.
        while (batches < properties.maxBatches) {
            val orphans = conversationRepository.findOrphanSessionIds(cutoff, properties.batchSize)
            if (orphans.isEmpty()) {
                break
            }
            orphans.forEach(sessionService::delete)
            deleted += orphans.size
            batches++
        }

        if (deleted > 0) {
            logger.info("Deleted {} orphan session(s) created before {}", deleted, cutoff)
        }
        return deleted
    }
}

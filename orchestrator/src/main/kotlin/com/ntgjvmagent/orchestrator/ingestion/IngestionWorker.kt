package com.ntgjvmagent.orchestrator.ingestion

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.IngestionJob
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.IngestionJobRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class IngestionWorker(
    private val jobRepo: IngestionJobRepository,
    private val jobService: IngestionJobService,
    private val knowledgeRepo: AgentKnowledgeRepository,
    private val knowledgeChunkService: KnowledgeChunkService,
    private val orchestrator: IngestionOrchestrator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val WORKER_FIXED_DELAY_MS = 1_000L
        private const val MAX_ERROR_MESSAGE_LENGTH = 2_000
    }

    @Scheduled(fixedDelay = WORKER_FIXED_DELAY_MS)
    fun processQueue() {
        val job = jobRepo.findFirstWithFetch(IngestionJobStatus.PENDING) ?: return
        execute(job)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun execute(job: IngestionJob) {
        try {
            log.info(
                "Processing ingestion job {} (agent={}, attempts={})",
                job.id,
                job.agent.id,
                job.attempts,
            )
            executeJob(job)
        } catch (ex: Exception) {
            log.error("Unexpected error: ${ex.message}", ex)
            handleFailure(job, job.knowledge, ex.message)
        }
    }

    @Transactional
    fun executeJob(job: IngestionJob) {
        val knowledge = job.knowledge
        val agentId = knowledge.agent.id!!
        val knowledgeId = knowledge.id!!

        jobService.markRunning(job)

        updateKnowledgeIngesting(knowledge)

        knowledgeChunkService.deleteAllChunks(agentId, knowledgeId)

        // Run ingestion handler (WEB_URL, FILE, INLINE...)
        orchestrator.process(knowledge)

        jobService.markSuccess(job)
        updateKnowledgeEmbeddingPending(knowledge)
    }

    private fun updateKnowledgeIngesting(k: AgentKnowledge) {
        k.status = KnowledgeStatus.INGESTING
        k.errorMessage = null
        knowledgeRepo.save(k)
    }

    private fun updateKnowledgeEmbeddingPending(k: AgentKnowledge) {
        k.status = KnowledgeStatus.EMBEDDING_PENDING
        k.lastProcessedAt = Instant.now()
        k.errorMessage = null
        knowledgeRepo.save(k)
    }

    private fun handleFailure(
        job: IngestionJob,
        knowledge: AgentKnowledge,
        message: String?,
    ) {
        val truncated = message?.take(MAX_ERROR_MESSAGE_LENGTH)

        jobService.incrementAttempts(job)

        val exceeded = job.attempts >= job.maxAttempts

        if (exceeded) {
            // ---------------- Permanently failed ----------------
            jobService.markFailed(job, truncated ?: "Unknown error")

            knowledge.status = KnowledgeStatus.FAILED
            knowledge.errorMessage = truncated
            knowledgeRepo.save(knowledge)
        } else {
            // ---------------- Requeue job ----------------
            job.status = IngestionJobStatus.PENDING
            job.errorMessage = truncated
            jobRepo.save(job)
        }
    }
}

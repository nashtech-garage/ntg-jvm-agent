package com.ntgjvmagent.orchestrator.ingestion.worker

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.IngestionJob
import com.ntgjvmagent.orchestrator.ingestion.job.IngestionJobService
import com.ntgjvmagent.orchestrator.ingestion.job.IngestionJobStatus
import com.ntgjvmagent.orchestrator.ingestion.orchestrator.IngestionOrchestrator
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.IngestionJobRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
class IngestionWorker(
    private val jobRepo: IngestionJobRepository,
    private val jobService: IngestionJobService,
    private val knowledgeRepo: AgentKnowledgeRepository,
    private val knowledgeChunkService: KnowledgeChunkService,
    private val orchestrator: IngestionOrchestrator,
    private val txManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val WORKER_FIXED_DELAY_MS = 1_000L
        private const val MAX_ERROR_MESSAGE_LENGTH = 2_000
    }

    @Scheduled(fixedDelay = WORKER_FIXED_DELAY_MS)
    fun processQueue() {
        val job = jobService.nextPending() ?: return
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
            log.error("Unexpected ingestion error: ${ex.message}", ex)
            handleFailure(job, job.knowledge, ex.message)
        }
    }

    /**
     * Main ingestion pipeline.
     * IMPORTANT: No long-lived DB transaction.
     */
    private fun executeJob(job: IngestionJob) {
        val knowledge = job.knowledge
        val agentId = knowledge.agent.id!!
        val knowledgeId = knowledge.id!!

        // STEP 1: Mark job RUNNING
        transactional {
            jobService.markRunning(job)
        }

        // STEP 2: Mark knowledge INGESTING
        transactional {
            knowledge.markIngesting()
            knowledgeRepo.save(knowledge)
        }

        // STEP 3: Delete old chunks
        transactional {
            knowledgeChunkService.deleteAllChunks(agentId, knowledgeId)
        }

        // STEP 4: Long-running ingestion (NO TX)
        orchestrator.process(knowledge)

        // STEP 5: Mark success
        transactional {
            jobService.markSuccess(job)
            knowledge.markEmbeddingPending()
            knowledgeRepo.save(knowledge)
        }
    }

    // ------------------------------------------------------------
    // Failure handling
    // ------------------------------------------------------------
    private fun handleFailure(
        job: IngestionJob,
        knowledge: AgentKnowledge,
        message: String?,
    ) {
        val truncated = message?.take(MAX_ERROR_MESSAGE_LENGTH)

        transactional {
            jobService.incrementAttempts(job)

            val exceeded = job.attempts >= job.maxAttempts

            if (exceeded) {
                log.error(
                    "Ingestion permanently failed: job={}, knowledge={}, reason={}",
                    job.id,
                    knowledge.id,
                    truncated,
                )

                jobService.markFailed(job, truncated ?: "Unknown error")
                knowledge.markFailed(truncated)
                knowledgeRepo.save(knowledge)
            } else {
                log.warn(
                    "Requeuing ingestion job {} (attempt {})",
                    job.id,
                    job.attempts,
                )

                job.status = IngestionJobStatus.PENDING
                job.errorMessage = truncated
                jobRepo.save(job)
            }
        }
    }

    // ------------------------------------------------------------
    // Transaction helper
    // ------------------------------------------------------------
    private fun <T> transactional(block: () -> T): T {
        val tmpl = TransactionTemplate(txManager)
        return tmpl.execute { block() }!!
    }
}

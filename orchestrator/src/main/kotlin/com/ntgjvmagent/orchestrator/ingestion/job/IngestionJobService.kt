package com.ntgjvmagent.orchestrator.ingestion.job

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.IngestionJob
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.IngestionJobRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IngestionJobService(
    private val repo: IngestionJobRepository,
    private val agentRepo: AgentRepository,
    private val knowledgeRepo: AgentKnowledgeRepository,
) {
    @Transactional
    fun enqueue(
        agentId: UUID,
        knowledgeId: UUID,
    ): IngestionJob {
        val agent = agentRepo.findByIdOrNull(agentId) ?: throw EntityNotFoundException("Agent not found")
        val knowledge =
            knowledgeRepo.findByIdOrNull(knowledgeId) ?: throw EntityNotFoundException("Knowledge not found")

        val job =
            IngestionJob(
                agent = agent,
                knowledge = knowledge,
            )

        return repo.save(job)
    }

    @Transactional
    fun markRunning(job: IngestionJob) {
        job.markRunning()
        repo.save(job)
    }

    @Transactional
    fun markSuccess(job: IngestionJob) {
        job.markSuccess()
        repo.save(job)
    }

    @Transactional
    fun markFailed(
        job: IngestionJob,
        error: String,
    ) {
        job.markFailed(error)
        repo.save(job)
    }

    @Transactional
    fun incrementAttempts(job: IngestionJob) {
        job.incrementAttempts()
        repo.save(job)
    }

    @Transactional(readOnly = true)
    fun nextPending(): IngestionJob? = repo.findFirstWithFetch(IngestionJobStatus.PENDING)
}

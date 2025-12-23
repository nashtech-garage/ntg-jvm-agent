package com.ntgjvmagent.orchestrator.embedding.job

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingJob
import com.ntgjvmagent.orchestrator.repository.EmbeddingJobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class EmbeddingJobService(
    private val jobRepo: EmbeddingJobRepository,
) {
    @Transactional
    fun takeJob(): EmbeddingJob? {
        val job =
            jobRepo
                .lockNextJobId(EmbeddingJobStatus.PENDING.name)
                .firstOrNull()
                ?.let { jobRepo.findByIdWithFetches(it) }

        job?.apply {
            status = EmbeddingJobStatus.RUNNING
            startedAt = Instant.now()
            // clear nextAttemptAt so we don’t block normal failure logic
            nextAttemptAt = null
        }

        return job?.let { jobRepo.save(it) }
    }

    @Transactional
    fun takeBatch(limit: Int): List<EmbeddingJob> {
        val jobs = mutableListOf<EmbeddingJob>()

        repeat(limit) {
            val job = takeJob() ?: return jobs
            jobs += job
        }

        return jobs
    }
}

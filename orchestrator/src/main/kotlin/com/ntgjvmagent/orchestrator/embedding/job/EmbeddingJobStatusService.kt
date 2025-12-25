package com.ntgjvmagent.orchestrator.embedding.job

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingJob
import com.ntgjvmagent.orchestrator.repository.EmbeddingJobRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeReadinessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

@Service
class EmbeddingJobStatusService(
    private val jobRepo: EmbeddingJobRepository,
    private val readinessService: KnowledgeReadinessService,
) {
    companion object {
        /** Maximum length for stored error messages. */
        private const val MAX_ERROR_MESSAGE_LEN = 2_000

        /** Base delay (ms) for exponential backoff (2^attempt × BASE_BACKOFF_MS). */
        private const val BASE_BACKOFF_MS = 1_000L // 1 second

        /** Minimum jitter added to delay (ms). */
        private const val MIN_JITTER_MS = 0L

        /** Maximum jitter added to delay (ms). */
        private const val MAX_JITTER_MS = 800L // 0–800ms jitter

        /** Maximum allowed retry delay (ms). */
        private const val MAX_BACKOFF_MS = 30_000L // cap at 30s

        /** Exponential growth base for backoff (2^attempt). */
        private const val BACKOFF_EXPONENT_BASE = 2.0
    }

    // ---------------------------------------------------------------------
    // SUCCESS
    // ---------------------------------------------------------------------
    @Transactional
    fun markSuccess(job: EmbeddingJob) {
        job.status = EmbeddingJobStatus.SUCCESS
        job.finishedAt = Instant.now()
        job.errorMessage = null
        job.nextAttemptAt = null

        jobRepo.save(job)
        readinessService.refreshReadiness(job.knowledge.id!!)
    }

    // ---------------------------------------------------------------------
    // INTERMEDIATE RETRY (e.g., rate limit)
    // ---------------------------------------------------------------------
    @Transactional
    fun markRetryLater(
        job: EmbeddingJob,
        ex: Throwable,
    ) {
        job.errorMessage = ex.message?.take(MAX_ERROR_MESSAGE_LEN)

        val delay = computeBackoff(job.attempts)
        job.nextAttemptAt = Instant.now().plus(delay)
        job.status = EmbeddingJobStatus.PENDING

        jobRepo.save(job)
    }

    // ---------------------------------------------------------------------
    // FAILURE (with max attempts logic)
    // ---------------------------------------------------------------------
    @Transactional
    fun markFailure(
        job: EmbeddingJob,
        ex: Throwable,
    ) {
        job.attempts++
        job.errorMessage = ex.message?.take(MAX_ERROR_MESSAGE_LEN)

        if (job.attempts >= job.maxAttempts) {
            // --- permanently failed ---
            job.status = EmbeddingJobStatus.FAILED
            job.finishedAt = Instant.now()
            job.nextAttemptAt = null
        } else {
            // --- retry again later ---
            val delay = computeBackoff(job.attempts)
            job.nextAttemptAt = Instant.now().plus(delay)
            job.status = EmbeddingJobStatus.PENDING
        }

        jobRepo.save(job)
        readinessService.refreshReadiness(job.knowledge.id!!)
    }

    // ---------------------------------------------------------------------
    // BACKOFF CALCULATION
    // ---------------------------------------------------------------------
    fun computeBackoff(attempt: Int): Duration {
        val exponential = BACKOFF_EXPONENT_BASE.pow(attempt.toDouble())
        val baseDelayMs = (exponential * BASE_BACKOFF_MS).toLong()

        val jitterMs =
            ThreadLocalRandom
                .current()
                .nextLong(MIN_JITTER_MS, MAX_JITTER_MS)

        val finalDelayMs =
            (baseDelayMs + jitterMs).coerceAtMost(MAX_BACKOFF_MS)

        return Duration.ofMillis(finalDelayMs)
    }
}

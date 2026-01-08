package com.ntgjvmagent.orchestrator.embedding.recovery

import com.ntgjvmagent.orchestrator.embedding.job.EmbeddingJobStatus
import com.ntgjvmagent.orchestrator.repository.EmbeddingJobRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class EmbeddingRecoveryService(
    private val embeddingJobRepo: EmbeddingJobRepository,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        resetRunningEmbeddingJobs()
    }

    private fun resetRunningEmbeddingJobs() {
        val runningJobs = embeddingJobRepo.findAllByStatus(EmbeddingJobStatus.RUNNING)

        if (runningJobs.isNotEmpty()) {
            log.warn(
                "Recovering ${runningJobs.size} embedding jobs stuck in RUNNING state due to unexpected shutdown...",
            )
        }

        runningJobs.forEach { job ->
            job.status = EmbeddingJobStatus.PENDING
            job.startedAt = null
            job.finishedAt = null
            job.errorMessage = "Server restarted before embedding completed. Retrying."
            job.updatedAt = Instant.now()
            embeddingJobRepo.save(job)
        }

        if (runningJobs.isNotEmpty()) {
            log.info("Recovered ${runningJobs.size} embedding jobs.")
        }
    }
}

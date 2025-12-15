package com.ntgjvmagent.orchestrator.ingestion

import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.IngestionJobRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class IngestionRecoveryService(
    private val jobRepo: IngestionJobRepository,
    private val knowledgeRepo: AgentKnowledgeRepository,
) : ApplicationRunner {
    companion object {
        private const val RECOVERY_JOB_MESSAGE =
            "Server restarted before ingestion completed. Retrying."

        private const val RECOVERY_KNOWLEDGE_MESSAGE =
            "Server restarted before ingestion completed."
    }

    private val log = LoggerFactory.getLogger(IngestionRecoveryService::class.java)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        log.info("IngestionRecoveryService starting...")

        val jobCount = resetRunningJobs()
        val knowledgeCount = resetStuckKnowledge()

        log.info(
            "IngestionRecoveryService completed. Jobs recovered=$jobCount, knowledge recovered=$knowledgeCount",
        )
    }

    /**
     * Reset ingestion jobs stuck in RUNNING → back to PENDING.
     */
    private fun resetRunningJobs(): Int {
        val runningJobs = jobRepo.findAllByStatus(IngestionJobStatus.RUNNING)

        if (runningJobs.isEmpty()) return 0

        log.warn("Recovering ${runningJobs.size} ingestion jobs stuck in RUNNING state...")

        runningJobs.forEach { job ->
            job.status = IngestionJobStatus.PENDING
            job.startedAt = null
            job.finishedAt = null
            job.errorMessage = RECOVERY_JOB_MESSAGE
            jobRepo.save(job)
        }

        log.info("Recovered ${runningJobs.size} ingestion jobs.")
        return runningJobs.size
    }

    /**
     * Reset knowledge stuck in INGESTING → back to PENDING.
     */
    private fun resetStuckKnowledge(): Int {
        val stuckList = knowledgeRepo.findAllByStatus(KnowledgeStatus.INGESTING)

        if (stuckList.isEmpty()) return 0

        log.warn("Recovering ${stuckList.size} knowledge entries stuck in INGESTING state...")

        stuckList.forEach { knowledge ->
            knowledge.status = KnowledgeStatus.PENDING
            knowledge.errorMessage = RECOVERY_KNOWLEDGE_MESSAGE
            knowledgeRepo.save(knowledge)
        }

        log.info("Recovered ${stuckList.size} knowledge entries.")
        return stuckList.size
    }
}

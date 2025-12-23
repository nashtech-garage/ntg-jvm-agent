package com.ntgjvmagent.orchestrator.ingestion.orchestrator

import com.ntgjvmagent.orchestrator.ingestion.job.IngestionJobService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IngestionLifecycleService(
    private val ingestionJobService: IngestionJobService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("taskExecutor")
    open fun triggerIngestionAsync(
        agentId: UUID,
        knowledgeId: UUID,
    ) {
        log.info(
            ">>> triggerIngestionAsync on thread '{}' for knowledge={}",
            Thread.currentThread().name,
            knowledgeId,
        )

        ingestionJobService.enqueue(agentId, knowledgeId)
    }
}

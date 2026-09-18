package com.ntgjvmagent.orchestrator.service.maintenance

import com.ntgjvmagent.orchestrator.service.PendingQuestionService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PendingQuestionCleanupJob(
    private val pendingQuestionService: PendingQuestionService,
) {
    @Scheduled(cron = "\${pending-question.cleanup-cron}", zone = "UTC")
    fun cleanup(): Int = pendingQuestionService.expireDue()
}

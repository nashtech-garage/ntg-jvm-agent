package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.EmbeddingJobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KnowledgeReadinessService(
    private val jobRepo: EmbeddingJobRepository,
    private val knowledgeRepo: AgentKnowledgeRepository,
) {
    @Transactional
    fun refreshReadiness(knowledgeId: UUID) {
        val unfinished = jobRepo.countUnfinished(knowledgeId)
        val failed = jobRepo.countFailed(knowledgeId)

        when {
            unfinished > 0 -> {
                knowledgeRepo.updateStatus(
                    knowledgeId,
                    KnowledgeStatus.EMBEDDING_PENDING,
                    null,
                )
            }

            failed > 0 -> {
                knowledgeRepo.updateStatus(
                    knowledgeId,
                    KnowledgeStatus.FAILED,
                    "Some embedding jobs failed ($failed)",
                )
            }

            else -> {
                knowledgeRepo.updateStatus(
                    knowledgeId,
                    KnowledgeStatus.READY,
                    null,
                )
            }
        }
    }
}

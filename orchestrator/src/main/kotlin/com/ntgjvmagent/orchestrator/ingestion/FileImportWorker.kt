package com.ntgjvmagent.orchestrator.ingestion

import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class FileImportWorker(
    private val importService: KnowledgeImportService,
    private val knowledgeRepo: AgentKnowledgeRepository,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Suppress("TooGenericExceptionCaught")
    @Async("taskExecutor")
    fun run(
        agentId: UUID,
        knowledgeId: UUID,
        file: MultipartFile,
    ) {
        try {
            importService.performImport(agentId, knowledgeId, file)
        } catch (ex: Exception) {
            log.error(
                "File import failed: agentId={}, knowledgeId={}, error={}",
                agentId,
                knowledgeId,
                ex.message,
                ex,
            )

            // Mark knowledge as FAILED
            failKnowledge(knowledgeId, ex)

            // DO NOT rethrow — @Async method will swallow exceptions anyway
        }
    }

    /**
     * Mark the knowledge record as FAILED and persist the error.
     */
    @Transactional
    fun failKnowledge(
        knowledgeId: UUID,
        ex: Exception,
    ) {
        val knowledge =
            knowledgeRepo.findByIdOrNull(knowledgeId)
                ?: run {
                    log.error("Cannot update knowledge {} to FAILED — not found", knowledgeId)
                    return
                }

        knowledge.markFailed(ex.message ?: "Unknown error")
        knowledgeRepo.save(knowledge)
    }
}

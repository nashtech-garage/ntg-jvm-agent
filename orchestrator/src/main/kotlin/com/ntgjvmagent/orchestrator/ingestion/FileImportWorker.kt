package com.ntgjvmagent.orchestrator.ingestion

import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class FileImportWorker(
    private val importService: KnowledgeImportService,
) {
    @Async("taskExecutor")
    fun run(
        agentId: UUID,
        knowledgeId: UUID,
        file: MultipartFile,
    ) {
        importService.performImport(agentId, knowledgeId, file)
    }
}

package com.ntgjvmagent.orchestrator.ingestion.handlers

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.ingestion.orchestrator.IngestionHandler
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import org.springframework.stereotype.Service

@Service
class FileIngestionHandler(
    private val knowledgeImportService: KnowledgeImportService,
) : IngestionHandler {
    override fun supports(type: KnowledgeSourceType) = type == KnowledgeSourceType.FILE

    override fun ingest(entity: AgentKnowledge) {
        knowledgeImportService.performImport(
            agentId = entity.agent.id!!,
            knowledgeId = entity.id!!,
        )
    }
}

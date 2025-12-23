package com.ntgjvmagent.orchestrator.ingestion.handlers

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.ingestion.orchestrator.IngestionHandler
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import org.springframework.stereotype.Service

@Service
class ApiIngestionHandler : IngestionHandler {
    override fun supports(type: KnowledgeSourceType) = type == KnowledgeSourceType.API

    override fun ingest(entity: AgentKnowledge) {
        //
    }
}

package com.ntgjvmagent.orchestrator.ingestion.handlers

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.ingestion.IngestionHandler
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import org.springframework.stereotype.Service

@Service
class SitemapIngestionHandler : IngestionHandler {
    override fun supports(type: KnowledgeSourceType) = type == KnowledgeSourceType.SITEMAP

    override fun ingest(entity: AgentKnowledge) {
        //
    }
}

package com.ntgjvmagent.orchestrator.ingestion

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.ingestion.handlers.ApiIngestionHandler
import com.ntgjvmagent.orchestrator.ingestion.handlers.DatabaseIngestionHandler
import com.ntgjvmagent.orchestrator.ingestion.handlers.FileIngestionHandler
import com.ntgjvmagent.orchestrator.ingestion.handlers.InlineIngestionHandler
import com.ntgjvmagent.orchestrator.ingestion.handlers.SitemapIngestionHandler
import com.ntgjvmagent.orchestrator.ingestion.handlers.WebUrlIngestionHandler
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import org.springframework.stereotype.Service

@Service
class IngestionOrchestrator(
    private val fileHandler: FileIngestionHandler,
    private val webUrlHandler: WebUrlIngestionHandler,
    private val sitemapHandler: SitemapIngestionHandler,
    private val inlineHandler: InlineIngestionHandler,
    private val databaseHandler: DatabaseIngestionHandler,
    private val apiHandler: ApiIngestionHandler,
) {
    fun process(knowledge: AgentKnowledge) {
        when (knowledge.sourceType) {
            KnowledgeSourceType.WEB_URL -> webUrlHandler.ingest(knowledge)
            KnowledgeSourceType.SITEMAP -> sitemapHandler.ingest(knowledge)
            KnowledgeSourceType.INLINE -> inlineHandler.ingest(knowledge)
            KnowledgeSourceType.FILE -> fileHandler.ingest(knowledge)
            KnowledgeSourceType.API -> apiHandler.ingest(knowledge)
            KnowledgeSourceType.DATABASE -> databaseHandler.ingest(knowledge)
        }
    }
}

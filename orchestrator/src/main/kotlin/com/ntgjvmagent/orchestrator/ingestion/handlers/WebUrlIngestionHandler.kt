package com.ntgjvmagent.orchestrator.ingestion.handlers

import com.ntgjvmagent.orchestrator.chunking.DocumentChunker
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.ingestion.orchestrator.IngestionHandler
import com.ntgjvmagent.orchestrator.ingestion.utils.ContentExtractor
import com.ntgjvmagent.orchestrator.ingestion.utils.WebPageFetcher
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import org.springframework.stereotype.Service

@Service
class WebUrlIngestionHandler(
    private val webPageFetcher: WebPageFetcher,
    private val documentChunker: DocumentChunker,
    private val knowledgeChunkService: KnowledgeChunkService,
) : IngestionHandler {
    override fun supports(type: KnowledgeSourceType) = type == KnowledgeSourceType.WEB_URL

    override fun ingest(entity: AgentKnowledge) {
        val url =
            checkNotNull(entity.sourceUri) {
                "WEB_URL knowledge missing source_uri"
            }

        val html = webPageFetcher.fetch(url)
        val text = ContentExtractor.extractMainContent(html)

        val baseMetadata =
            mapOf(
                "sourceType" to "WEB_URL",
                "url" to url,
            )

        val chunks =
            documentChunker.chunkText(
                text = text,
                metadata = baseMetadata,
            )

        chunks.forEachIndexed { index, chunk ->
            knowledgeChunkService.createChunkAndEnqueueEmbedding(
                agentId = entity.agent.id!!,
                knowledgeId = entity.id!!,
                content = chunk.text ?: "",
                metadata = baseMetadata + chunk.metadata,
                chunkOrder = index + 1,
            )
        }
    }
}

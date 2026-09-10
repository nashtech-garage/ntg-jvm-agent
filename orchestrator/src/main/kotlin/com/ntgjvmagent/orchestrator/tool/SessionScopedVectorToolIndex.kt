package com.ntgjvmagent.orchestrator.tool

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.tool.toolsearch.ToolIndex
import org.springframework.ai.tool.toolsearch.ToolReference
import org.springframework.ai.tool.toolsearch.ToolSearchRequest
import org.springframework.ai.tool.toolsearch.ToolSearchResponse
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import java.time.Clock
import java.util.UUID

class SessionScopedVectorToolIndex(
    private val vectorStore: VectorStore,
    private val clock: Clock = Clock.systemUTC(),
) : ToolIndex {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun clearIndex(sessionId: String) {
        vectorStore.delete(sessionFilter(sessionId))
    }

    override fun indexTool(
        sessionId: String,
        toolReference: ToolReference,
    ) = indexTools(sessionId, listOf(toolReference))

    override fun indexTools(
        sessionId: String,
        toolReferences: List<ToolReference>,
    ) {
        if (toolReferences.isEmpty()) return

        vectorStore.add(
            toolReferences.map { reference ->
                Document(
                    UUID.randomUUID().toString(),
                    reference.summary(),
                    mapOf(
                        SESSION_ID_METADATA to sessionId,
                        TOOL_NAME_METADATA to reference.toolName(),
                        TOOL_DESCRIPTION_METADATA to reference.summary(),
                        INDEX_MARKER_METADATA to true,
                        INDEXED_AT_METADATA to clock.millis(),
                    ),
                )
            },
        )
    }

    override fun search(request: ToolSearchRequest): ToolSearchResponse {
        if (request.categoryFilter() != null) {
            logger.warn("Vector tool search does not support category filters")
        }
        val references =
            vectorStore
                .similaritySearch(
                    SearchRequest
                        .builder()
                        .query(request.query())
                        .topK(request.maxResults() ?: DEFAULT_MAX_RESULTS)
                        .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                        .filterExpression(sessionFilter(request.sessionId()))
                        .build(),
                ).map { document ->
                    ToolReference
                        .builder()
                        .toolName(requireNotNull(document.metadata[TOOL_NAME_METADATA]) as String)
                        .summary(requireNotNull(document.metadata[TOOL_DESCRIPTION_METADATA]) as String)
                        .relevanceScore(document.score ?: 0.0)
                        .build()
                }

        return ToolSearchResponse
            .builder()
            .toolReferences(references)
            .totalMatches(references.size)
            .searchMetadata(
                ToolSearchResponse.SearchMetadata
                    .builder()
                    .searchType(javaClass.simpleName)
                    .query(request.query())
                    .build(),
            ).build()
    }

    private fun sessionFilter(sessionId: String) =
        FilterExpressionBuilder()
            .eq(SESSION_ID_METADATA, sessionId)
            .build()

    companion object {
        const val SESSION_ID_METADATA = "toolSearchSessionId"
        const val TOOL_NAME_METADATA = "toolSearchToolName"
        const val TOOL_DESCRIPTION_METADATA = "toolSearchToolDescription"

        /**
         * Marks a document as belonging to the tool index.
         *
         * The sweep needs a term that is false for every other document in this shared store,
         * and a comparison against a key those documents do not have will not do: SimpleVectorStore
         * evaluates `LT` on an absent key as true, so an age filter on its own deletes every RAG
         * chunk alongside the stale tool documents. Equality against a constant is false when the
         * key is missing, on every store.
         */
        const val INDEX_MARKER_METADATA = "toolSearchIndex"

        /** When the document was written, as epoch millis, so ToolIndexCleanupJob can sweep by age. */
        const val INDEXED_AT_METADATA = "toolSearchIndexedAt"
        private const val DEFAULT_MAX_RESULTS = 10
        private const val DEFAULT_SIMILARITY_THRESHOLD = 0.2
    }
}

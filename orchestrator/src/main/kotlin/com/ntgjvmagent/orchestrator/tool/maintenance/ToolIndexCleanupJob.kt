package com.ntgjvmagent.orchestrator.tool.maintenance

import com.ntgjvmagent.orchestrator.config.ToolSearchIndexProperties
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.tool.SessionScopedVectorToolIndex
import org.slf4j.LoggerFactory
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

/**
 * Deletes tool search documents that are older than the configured TTL.
 *
 * The advisor indexes a session's tools on first use and drops them again through its eviction
 * strategy, but that strategy lives in memory: a restart loses it, and every session indexed
 * before the restart keeps its documents in the vector store for good. Since the store is shared
 * with the RAG chunks, that growth lands in the same table those are retrieved from.
 *
 * The sweep is one filtered delete rather than a per-session loop: the session id is metadata,
 * not a column, so deleting session by session would scan the table once per session.
 *
 * Deleting documents a running instance still considers indexed would leave that session's tool
 * search returning nothing until the tool set changed or the process restarted. What rules that
 * out is [ToolSearchIndexProperties.maxIndexAge] being shorter than
 * [ToolSearchIndexProperties.documentTtl]: both ages start at the same indexing, so anything old
 * enough to be swept here was already evicted and re-indexed in memory.
 */
@Component
@ConditionalOnProperty(value = ["tool-search.index.cleanup-enabled"], havingValue = "true", matchIfMissing = true)
class ToolIndexCleanupJob(
    private val vectorStoreService: VectorStoreService,
    private val properties: ToolSearchIndexProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${tool-search.index.cleanup-cron}", zone = "UTC")
    fun cleanup() {
        val cutoff = Instant.now(clock).minus(properties.documentTtl).toEpochMilli()

        // The marker term is not redundant: SimpleVectorStore evaluates a comparison against an
        // absent key as true, so the age term alone would delete every RAG chunk in this shared
        // store. Equality against a constant is false when the key is missing.
        vectorStoreService.getVectorStore().delete(
            Filter.Expression(
                Filter.ExpressionType.AND,
                Filter.Expression(
                    Filter.ExpressionType.EQ,
                    Filter.Key(SessionScopedVectorToolIndex.INDEX_MARKER_METADATA),
                    Filter.Value(true),
                ),
                Filter.Expression(
                    Filter.ExpressionType.LT,
                    Filter.Key(SessionScopedVectorToolIndex.INDEXED_AT_METADATA),
                    Filter.Value(cutoff),
                ),
            ),
        )

        logger.info("Swept tool search documents indexed before {}", Instant.ofEpochMilli(cutoff))
    }
}

package com.ntgjvmagent.orchestrator.unit.tool

import com.ntgjvmagent.orchestrator.config.ToolSearchIndexProperties
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.tool.SessionScopedVectorToolIndex
import com.ntgjvmagent.orchestrator.tool.maintenance.ToolIndexCleanupJob
import com.ntgjvmagent.orchestrator.unit.support.BagOfWordsEmbeddingModel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.document.Document
import org.springframework.ai.tool.toolsearch.ToolReference
import org.springframework.ai.tool.toolsearch.ToolSearchRequest
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * Runs against a real SimpleVectorStore so the age filter is actually evaluated rather than
 * asserted against a mock's recorded arguments.
 */
class ToolIndexCleanupJobTest {
    private val ttl = Duration.ofDays(30)
    private val now = Instant.parse("2026-06-01T00:00:00Z")

    @Test
    fun `documents past the ttl are swept and fresher ones are kept`() {
        val store = store()
        index(store, at = now.minus(Duration.ofDays(31)), session = "stale", tool = "refundOrder")
        index(store, at = now.minus(Duration.ofDays(29)), session = "fresh", tool = "refundInvoice")

        job(store).cleanup()

        assertEquals(emptyList<String>(), toolNames(store, "stale"))
        assertEquals(listOf("refundInvoice"), toolNames(store, "fresh"))
    }

    @Test
    fun `documents without the tool search timestamp are never touched`() {
        val store = store()
        index(store, at = now.minus(Duration.ofDays(31)), session = "stale", tool = "refundOrder")
        // A RAG chunk: same store, no toolSearchIndexedAt.
        store.add(listOf(Document(UUID.randomUUID().toString(), "refund policy", mapOf("knowledgeId" to "k1"))))

        job(store).cleanup()

        val remaining =
            store
                .similaritySearch(
                    SearchRequest
                        .builder()
                        .query("refund")
                        .topK(10)
                        .build(),
                ).orEmpty()
        assertEquals(listOf("k1"), remaining.map { it.metadata["knowledgeId"] })
    }

    @Test
    fun `an index age at or beyond the document ttl is rejected at startup`() {
        // Without this ordering the sweep could delete documents a running instance still
        // considers indexed, blinding that session's tool search until the process restarted.
        assertThrows<IllegalArgumentException> {
            ToolSearchIndexProperties(documentTtl = Duration.ofDays(7), maxIndexAge = Duration.ofDays(7))
        }
    }

    private fun job(store: VectorStore) =
        ToolIndexCleanupJob(
            vectorStoreService = mockk<VectorStoreService> { every { getVectorStore() } returns store },
            properties = ToolSearchIndexProperties(documentTtl = ttl),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

    private fun index(
        store: VectorStore,
        at: Instant,
        session: String,
        tool: String,
    ) = SessionScopedVectorToolIndex(store, Clock.fixed(at, ZoneOffset.UTC))
        .indexTool(
            session,
            ToolReference
                .builder()
                .toolName(tool)
                .summary("Refund something")
                .build(),
        )

    private fun toolNames(
        store: VectorStore,
        session: String,
    ): List<String> =
        SessionScopedVectorToolIndex(store)
            .search(ToolSearchRequest(session, "refund something", 5, null))
            .toolReferences()
            .map { it.toolName() }

    private fun store(): VectorStore = SimpleVectorStore.builder(BagOfWordsEmbeddingModel).build()
}

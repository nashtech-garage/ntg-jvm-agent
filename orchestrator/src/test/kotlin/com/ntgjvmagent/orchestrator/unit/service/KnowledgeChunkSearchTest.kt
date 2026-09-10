package com.ntgjvmagent.orchestrator.unit.service

import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.tool.SessionScopedVectorToolIndex
import com.ntgjvmagent.orchestrator.unit.support.BagOfWordsEmbeddingModel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import java.util.UUID

/**
 * The tool search index shares the RAG vector store, so chunk search must filter inside the
 * query. These tests populate a real [SimpleVectorStore] -- its filter evaluator is the same
 * code path shape as a production store -- with tool documents that outrank the chunk.
 */
class KnowledgeChunkSearchTest {
    private val agentId = UUID.randomUUID()
    private val knowledgeId = UUID.randomUUID()

    @Test
    fun `tool index documents do not crowd out knowledge chunks`() {
        val store = store()
        // Five tool documents matching the query far better than the chunk does; with the
        // default top-K of 4 an unfiltered search would return only these and the post-filter
        // would then leave the caller with nothing.
        store.add(
            (1..5).map { index ->
                Document(
                    UUID.randomUUID().toString(),
                    "refund refund refund policy tool $index",
                    mapOf(SessionScopedVectorToolIndex.SESSION_ID_METADATA to UUID.randomUUID().toString()),
                )
            },
        )
        val chunkId = UUID.randomUUID()
        store.add(
            listOf(
                Document(
                    chunkId.toString(),
                    "refund window is thirty days",
                    mapOf(KnowledgeChunkService.KNOWLEDGE_ID_METADATA to knowledgeId.toString()),
                ),
            ),
        )

        val results = service(store).searchSimilarChunks(agentId, knowledgeId, "refund policy")

        assertEquals(listOf(chunkId), results.map { it.id })
    }

    @Test
    fun `an agent without active knowledge searches nothing`() {
        val store = store()
        store.add(
            listOf(
                Document(
                    UUID.randomUUID().toString(),
                    "refund window is thirty days",
                    mapOf(KnowledgeChunkService.KNOWLEDGE_ID_METADATA to UUID.randomUUID().toString()),
                ),
            ),
        )
        val chunkRepo =
            mockk<KnowledgeChunkRepository> {
                every { findAllKnowledgeIdsActiveByAgent(agentId) } returns emptyList()
            }

        val results = service(store, chunkRepo).searchSimilarChunks(agentId, knowledgeId, "refund policy")

        assertEquals(emptyList<UUID>(), results.map { it.id })
    }

    private fun service(
        store: VectorStore,
        chunkRepo: KnowledgeChunkRepository =
            mockk {
                every { findAllKnowledgeIdsActiveByAgent(agentId) } returns listOf(knowledgeId)
            },
    ): KnowledgeChunkService =
        KnowledgeChunkService(
            chunkRepo = chunkRepo,
            knowledgeRepo =
                mockk<AgentKnowledgeRepository> {
                    every { existsByIdAndAgentId(knowledgeId, agentId) } returns true
                },
            vectorStoreService =
                mockk<VectorStoreService> {
                    every { getVectorStore() } returns store
                },
            embeddingQueueService = mockk(),
            embeddingJobRepo = mockk(),
        )

    private fun store(): VectorStore = SimpleVectorStore.builder(BagOfWordsEmbeddingModel).build()
}

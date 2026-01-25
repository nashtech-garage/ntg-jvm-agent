package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.model.ProviderType
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test

class KnowledgeChunkServiceIT
    @Autowired
    constructor(
        private val chunkService: KnowledgeChunkService,
        private val chunkRepo: KnowledgeChunkRepository,
        private val knowledgeRepo: AgentKnowledgeRepository,
        private val agentRepo: AgentRepository,
    ) : BaseIntegrationTest() {
        private lateinit var agent: Agent
        private lateinit var knowledge: AgentKnowledge

        @BeforeEach
        fun setup() {
            chunkRepo.deleteAllInBatch()
            knowledgeRepo.deleteAllInBatch()
            agentRepo.deleteAllInBatch()
            agentRepo.flush()
            knowledgeRepo.flush()
            chunkRepo.flush()

            agent =
                agentRepo.save(
                    Agent(
                        name = "Test Agent",
                        provider = ProviderType.OPENAI,
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        model = "gpt-4",
                    ),
                )

            knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "K1",
                        sourceType = KnowledgeSourceType.INLINE,
                        sourceUri = null,
                        metadata = emptyMap(),
                    ).apply { active = true },
                )
        }

        @Test
        fun `addChunk should persist chunk content without embeddings`() {
            val content = "This is a test chunk"

            val response =
                chunkService.createChunkAndEnqueueEmbedding(
                    agentId = agent.id!!,
                    knowledgeId = knowledge.id!!,
                    content = content,
                )

            val persisted = chunkRepo.findById(response.id).orElseThrow()

            assertEquals(content, persisted.content)
            assertEquals(knowledge.id, persisted.knowledge.id)
            assertEquals(1, persisted.chunkOrder)

            // 🔒 Embeddings must NOT be stored in DB anymore
            // If columns still exist during migration, they must remain null
            persisted::class
                .members
                .filter { it.name.startsWith("embedding") }
                .forEach {
                    val value = it.call(persisted)
                    assertNull(value, "Embedding must not be persisted in knowledge_chunk")
                }
        }

        @Test
        fun `searchSimilarChunks should return empty when vector store is mocked`() {
            chunkService.createChunkAndEnqueueEmbedding(
                agentId = agent.id!!,
                knowledgeId = knowledge.id!!,
                content = "Chunk one",
            )

            chunkService.createChunkAndEnqueueEmbedding(
                agentId = agent.id!!,
                knowledgeId = knowledge.id!!,
                content = "Chunk two",
            )

            val results =
                chunkService.searchSimilarChunks(
                    agentId = agent.id!!,
                    knowledgeId = knowledge.id!!,
                    query = "Chunk",
                    topK = 2,
                )

            // Vector store is mocked in TestEmbeddingConfig → no results
            assertTrue(results.isEmpty())
        }
    }

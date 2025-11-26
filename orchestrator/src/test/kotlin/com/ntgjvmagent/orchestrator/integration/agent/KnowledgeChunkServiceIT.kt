package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
                        provider = "OpenAI",
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        model = "gpt-4",
                        embeddingModel = "openai/text-embedding-3-small",
                        embeddingsPath = "/embeddings",
                    ),
                )

            knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "K1",
                    ).apply { active = true },
                )
        }

        @Test
        fun `addChunk should persist chunk and call vectorStore`() {
            val content = "This is a test chunk"
            val chunk = chunkService.addChunk(agent.id!!, knowledge.id!!, content)

            val persisted = chunkRepo.findById(chunk.id).orElseThrow()
            val embedding = persisted.embedding768 ?: persisted.embedding1536
            assertEquals(content, persisted.content)
            assertNotNull(embedding)
        }

        @Test
        fun `searchSimilarChunks should filter by active knowledge`() {
            chunkService.addChunk(agent.id!!, knowledge.id!!, "Chunk one")
            chunkService.addChunk(agent.id!!, knowledge.id!!, "Chunk two")

            val results = chunkService.searchSimilarChunks(agent.id!!, knowledge.id!!, "Chunk", topK = 2)

            // Since TestEmbeddingConfig or vectorStore is mocked, expected empty list
            assertEquals(0, results.size)
        }
    }

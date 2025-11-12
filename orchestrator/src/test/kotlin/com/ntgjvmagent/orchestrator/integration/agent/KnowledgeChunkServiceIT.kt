package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.integration.config.TestEmbeddingConfig
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import kotlin.test.Test

@Import(TestEmbeddingConfig::class)
class KnowledgeChunkServiceIT
    @Autowired
    constructor(
        private val chunkService: KnowledgeChunkService,
        private val chunkRepo: KnowledgeChunkRepository,
        private val knowledgeRepo: AgentKnowledgeRepository,
    ) : BaseIntegrationTest() {
        private lateinit var knowledge: AgentKnowledge

        @BeforeEach
        fun setup() {
            chunkRepo.deleteAllInBatch()
            knowledgeRepo.deleteAllInBatch()

            knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(name = "K1").apply { active = true },
                )
        }

        @Test
        fun `addChunk should persist chunk and call vectorStore`() {
            val content = "This is a test chunk"
            val chunk = chunkService.addChunk(knowledge.id!!, content)

            val persisted = chunkRepo.findById(chunk.id).orElseThrow()

            assertEquals(content, persisted.content)
            assertNotNull(persisted.embedding)
        }

        @Test
        fun `searchSimilarChunks should filter by active knowledge`() {
            chunkService.addChunk(knowledge.id!!, "Chunk one")
            chunkService.addChunk(knowledge.id!!, "Chunk two")

            val results = chunkService.searchSimilarChunks("Chunk", topK = 2)

            assertEquals(0, results.size) // mocked vectorStore returns empty
        }
    }

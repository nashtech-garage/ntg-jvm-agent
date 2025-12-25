package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.embedding.job.EmbeddingJobStatus
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.EmbeddingJobRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnowledgeImportServiceIT
    @Autowired
    constructor(
        private val service: KnowledgeImportService,
        private val knowledgeRepo: AgentKnowledgeRepository,
        private val chunkRepo: KnowledgeChunkRepository,
        private val agentRepo: AgentRepository,
        private val embeddingJobRepo: EmbeddingJobRepository,
    ) : BaseIntegrationTest() {
        private lateinit var agent: Agent
        private lateinit var knowledge: AgentKnowledge

        @BeforeEach
        fun setUp() {
            embeddingJobRepo.deleteAll()
            chunkRepo.deleteAll()
            knowledgeRepo.deleteAll()
            agentRepo.deleteAll()

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
                        name = "Test Knowledge",
                        sourceType = KnowledgeSourceType.WEB_URL,
                        sourceUri = "http://example.com",
                        metadata = emptyMap(),
                    ).apply { active = true },
                )
        }

        // =============================================================
        // Helper Assertions
        // =============================================================

        private fun assertKnowledgeLifecycle(knowledgeId: UUID) {
            val updated = knowledgeRepo.findById(knowledgeId).get()

            assertEquals(KnowledgeStatus.EMBEDDING_PENDING, updated.status)
            assertNull(updated.errorMessage)
        }

        private fun assertEmbeddingJobLifecycle(
            agentId: UUID,
            knowledgeId: UUID,
            expectedChunks: Int,
        ) {
            val jobs =
                embeddingJobRepo
                    .findAllByKnowledgeIdAndKnowledgeAgentIdOrderByCreatedAtAsc(knowledgeId, agentId)

            assertEquals(expectedChunks, jobs.size)

            jobs.forEach {
                assertEquals(EmbeddingJobStatus.PENDING, it.status)
                assertEquals(0, it.attempts)
                assertNull(it.errorMessage)
                assertNotNull(it.createdAt)
            }
        }

        private fun assertChunksOrdered(chunks: List<KnowledgeChunk>) {
            val firstOrder = chunks.minOf { it.chunkOrder }
            chunks.forEachIndexed { index, chunk ->
                assertEquals(firstOrder + index, chunk.chunkOrder)
            }
        }

        // =============================================================
        // Tests
        // =============================================================

        @Test
        fun `importDocument should save txt file chunks and update lifecycle`() {
            val content =
                """
                This is a test document for chunking.
                It should produce multiple chunks when the chunk size is small.
                We are testing whether the chunker splits text correctly based on the chunk size property.
                Each sentence should ideally appear in a separate chunk to verify the correctness.
                """.trimIndent()

            val file =
                MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    content.toByteArray(),
                )

            val response = service.performImport(agent.id!!, knowledge.id!!, file)

            assertTrue(response > 1)

            val savedChunks =
                chunkRepo
                    .findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(knowledge.id!!, agent.id!!)
            assertEquals(response, savedChunks.size)
            assertTrue(savedChunks.any { it.content.contains("multiple chunks") })

            assertChunksOrdered(savedChunks)

            // Lifecycles
            assertKnowledgeLifecycle(knowledge.id!!)
            assertEmbeddingJobLifecycle(agent.id!!, knowledge.id!!, savedChunks.size)
        }

        @Test
        fun `importDocument should save pdf file chunks and update lifecycle`() {
            val pdfBytes =
                ByteArrayOutputStream().use { out ->
                    val document = PDDocument()
                    val page = PDPage()
                    document.addPage(page)

                    val contentStream = PDPageContentStream(document, page)
                    contentStream.beginText()
                    contentStream.setFont(PDType1Font.HELVETICA, 12f)
                    contentStream.newLineAtOffset(100f, 700f)
                    contentStream.showText("PDF test content for chunking.")
                    contentStream.endText()
                    contentStream.close()

                    document.save(out)
                    document.close()

                    out.toByteArray()
                }

            val file =
                MockMultipartFile(
                    "file",
                    "test.pdf",
                    "application/pdf",
                    pdfBytes,
                )

            val response = service.performImport(agent.id!!, knowledge.id!!, file)

            assertTrue(response > 0)

            val savedChunks =
                chunkRepo
                    .findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(knowledge.id!!, agent.id!!)

            assertEquals(response, savedChunks.size)
            assertTrue(savedChunks.any { it.content.contains("PDF test content") })

            assertChunksOrdered(savedChunks)

            // Lifecycles
            assertKnowledgeLifecycle(knowledge.id!!)
            assertEmbeddingJobLifecycle(agent.id!!, knowledge.id!!, savedChunks.size)
        }

        @Test
        fun `importDocument should use default profile for unknown extension and update lifecycle`() {
            val content =
                "This is a test file with an unknown extension to trigger default profile."

            val file =
                MockMultipartFile(
                    "file",
                    "unknown.xyz",
                    "text/plain",
                    content.toByteArray(),
                )

            val response = service.performImport(agent.id!!, knowledge.id!!, file)

            assertTrue(response >= 1)

            val savedChunks =
                chunkRepo
                    .findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(knowledge.id!!, agent.id!!)

            assertChunksOrdered(savedChunks)

            // Lifecycles
            assertKnowledgeLifecycle(knowledge.id!!)
            assertEmbeddingJobLifecycle(agent.id!!, knowledge.id!!, savedChunks.size)
        }
    }

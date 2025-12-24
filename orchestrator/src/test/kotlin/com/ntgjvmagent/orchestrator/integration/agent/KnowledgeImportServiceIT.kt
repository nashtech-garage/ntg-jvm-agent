package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.embedding.job.EmbeddingJobStatus
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.EmbeddingJobRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeService
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import com.ntgjvmagent.orchestrator.storage.core.ObjectStorage
import com.ntgjvmagent.orchestrator.viewmodel.KnowledgeImportingResponseVm
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Transactional
class KnowledgeImportServiceIT
    @Autowired
    constructor(
        private val service: KnowledgeImportService,
        private val agentKnowledgeService: AgentKnowledgeService,
        private val knowledgeRepo: AgentKnowledgeRepository,
        private val chunkRepo: KnowledgeChunkRepository,
        private val agentRepo: AgentRepository,
        private val embeddingJobRepo: EmbeddingJobRepository,
        private val objectStorage: ObjectStorage,
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
                        provider = ProviderType.OPENAI,
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        model = "gpt-4",
                        embeddingModel = "openai/text-embedding-3-small",
                        embeddingsPath = "/embeddings",
                    ),
                )
        }

        // ---------------------------------------------------------
        // Helpers
        // ---------------------------------------------------------

        private fun uploadKnowledge(
            fileName: String,
            contentType: String,
            bytes: ByteArray,
        ): AgentKnowledge {
            val file =
                MockMultipartFile(
                    "file",
                    fileName,
                    contentType,
                    bytes,
                )

            val created =
                agentKnowledgeService.createFileKnowledge(
                    agentId = agent.id!!,
                    originalFileName = fileName,
                    metadata = emptyMap(),
                    file = file,
                )

            return knowledgeRepo.findById(created.id).get()
        }

        // ---------------------------------------------------------
        // Tests
        // ---------------------------------------------------------

        @Test
        fun `importDocument should save txt file chunks and update lifecycle`() {
            val content =
                """
                This is a test document for chunking.
                It should produce multiple chunks when the chunk size is small.
                We are testing whether the chunker splits text correctly based on the chunk size property.
                Each sentence should ideally appear in a separate chunk to verify the correctness.
                """.trimIndent()

            knowledge = uploadKnowledge("test.txt", "text/plain", content.toByteArray())

            val response = service.performImport(agent.id!!, knowledge.id!!)

            assertTrue(response > 1)

            val savedChunks =
                chunkRepo.findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(
                    knowledge.id!!,
                    agent.id!!,
                )

            assertEquals(response, savedChunks.size)
            assertTrue(savedChunks.any { it.content.contains("multiple chunks") })

            assertKnowledgeLifecycle(knowledge.id!!)
            assertEmbeddingJobLifecycle(agent.id!!, knowledge.id!!, savedChunks.size)
        }

        @Test
        fun `importDocument should save pdf file chunks and update lifecycle`() {
            val pdfBytes =
                ByteArrayOutputStream().use { out ->
                    PDDocument().use { doc ->
                        val page = PDPage()
                        doc.addPage(page)
                        PDPageContentStream(doc, page).use {
                            it.beginText()
                            it.setFont(PDType1Font.HELVETICA, 12f)
                            it.newLineAtOffset(100f, 700f)
                            it.showText("PDF test content for chunking.")
                            it.endText()
                        }
                        doc.save(out)
                    }
                    out.toByteArray()
                }

            knowledge = uploadKnowledge("test.pdf", "application/pdf", pdfBytes)

            val response = service.performImport(agent.id!!, knowledge.id!!)

            assertTrue(response > 0)

            val savedChunks =
                chunkRepo.findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(
                    knowledge.id!!,
                    agent.id!!,
                )

            assertEquals(response, savedChunks.size)
            assertTrue(savedChunks.any { it.content.contains("PDF test content") })
            assertKnowledgeLifecycle(knowledge.id!!)
            assertEmbeddingJobLifecycle(agent.id!!, knowledge.id!!, savedChunks.size)
        }

        @Test
        fun `importDocument should throw if file missing from storage`() {
            val content = "some text".toByteArray()

            knowledge = uploadKnowledge("missing.txt", "text/plain", content)

            // Simulate missing file
            objectStorage.delete(knowledge.storageKey!!)

            val ex =
                assertThrows<IllegalStateException> {
                    service.performImport(agent.id!!, knowledge.id!!)
                }

            assertTrue(ex.message!!.contains("Stored file not found"))
        }

        // ---------------------------------------------------------
        // Assertions
        // ---------------------------------------------------------

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
                embeddingJobRepo.findAllByKnowledgeIdAndKnowledgeAgentIdOrderByCreatedAtAsc(
                    knowledgeId,
                    agentId,
                )

            assertEquals(expectedChunks, jobs.size)
            jobs.forEach {
                assertEquals(EmbeddingJobStatus.PENDING, it.status)
                assertEquals(0, it.attempts)
                assertNull(it.errorMessage)
            }
        }
    }

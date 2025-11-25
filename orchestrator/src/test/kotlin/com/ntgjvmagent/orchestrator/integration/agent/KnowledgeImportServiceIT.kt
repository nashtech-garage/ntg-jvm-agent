package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import com.ntgjvmagent.orchestrator.viewmodel.KnowledgeImportingResponseVm
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnowledgeImportServiceIT
    @Autowired
    constructor(
        private val service: KnowledgeImportService,
        private val knowledgeRepo: AgentKnowledgeRepository,
        private val chunkRepo: KnowledgeChunkRepository,
        private val agentRepo: AgentRepository,
    ) : BaseIntegrationTest() {
        private lateinit var agent: Agent
        private lateinit var knowledge: AgentKnowledge

        @BeforeEach
        fun setUp() {
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
                        model = "gpt-4",
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        embeddingsPath = "/embeddings",
                        embeddingModel = "openai/text-embedding-3-small",
                    ),
                )

            knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Test Knowledge",
                        sourceType = "manual",
                        sourceUri = "http://example.com",
                        metadata = emptyMap(),
                        embeddingModel = "text-embedding-3-small",
                    ).apply { active = true },
                )
        }

        @Test
        fun `importDocument should save txt file chunks to DB`() {
            val content =
                "This is a test document for chunking. " +
                    "It should produce multiple chunks when the chunk size is small.\n" +
                    "We are testing whether the chunker splits text correctly based on the chunk size property.\n" +
                    "Each sentence should ideally appear in a separate chunk to verify the correctness."
            val file =
                MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    content.toByteArray(StandardCharsets.UTF_8),
                )

            val response: KnowledgeImportingResponseVm = service.importDocument(agent.id!!, knowledge.id!!, file)

            assertEquals("test.txt", response.originalFilename)
            assertTrue(response.numberOfSegment > 1, "Expected multiple chunks for test.txt")

            val savedChunks =
                chunkRepo.findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(
                    knowledge.id!!,
                    agent.id!!,
                )
            assertEquals(response.numberOfSegment, savedChunks.size)
            assertTrue(savedChunks.any { it.content.contains("It should produce multiple chunks") })

            val firstOrder = savedChunks.minOf { it.chunkOrder }
            savedChunks.forEachIndexed { index, chunk ->
                assertEquals(firstOrder + index, chunk.chunkOrder)
            }
        }

        @Test
        fun `importDocument should save pdf file chunks to DB`() {
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

            val response = service.importDocument(agent.id!!, knowledge.id!!, file)

            assertEquals("test.pdf", response.originalFilename)
            assertTrue(response.numberOfSegment > 0, "Expected at least one chunk for PDF")

            val savedChunks =
                chunkRepo.findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(
                    knowledge.id!!,
                    agent.id!!,
                )
            assertEquals(response.numberOfSegment, savedChunks.size)
            assertTrue(savedChunks.any { it.content.contains("PDF test content") })

            val firstOrder = savedChunks.minOf { it.chunkOrder }
            savedChunks.forEachIndexed { index, chunk ->
                assertEquals(firstOrder + index, chunk.chunkOrder)
            }
        }

        @Test
        fun `importDocument should handle unknown file extension using default profile`() {
            val content = "This is a test file with an unknown extension to trigger default profile."
            val file =
                MockMultipartFile(
                    "file",
                    "unknown.xyz",
                    "text/plain",
                    content.toByteArray(StandardCharsets.UTF_8),
                )

            val response = service.importDocument(agent.id!!, knowledge.id!!, file)

            assertEquals("unknown.xyz", response.originalFilename)
            assertTrue(response.numberOfSegment >= 1)

            val savedChunks =
                chunkRepo.findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(
                    knowledge.id!!,
                    agent.id!!,
                )
            val firstOrder = savedChunks.minOf { it.chunkOrder }
            savedChunks.forEachIndexed { index, chunk ->
                assertEquals(firstOrder + index, chunk.chunkOrder)
            }
        }
    }

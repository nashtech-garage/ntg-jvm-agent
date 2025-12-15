package com.ntgjvmagent.orchestrator.unit.agent

import com.ntgjvmagent.orchestrator.chunking.ChunkerProfileDetector
import com.ntgjvmagent.orchestrator.chunking.DocumentChunker
import com.ntgjvmagent.orchestrator.chunking.DocumentTextExtractor
import com.ntgjvmagent.orchestrator.config.ChunkerProperties
import com.ntgjvmagent.orchestrator.dto.response.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KnowledgeImportServiceTest {
    private lateinit var chunkService: KnowledgeChunkService
    private lateinit var knowledgeRepo: AgentKnowledgeRepository
    private lateinit var documentChunker: DocumentChunker
    private lateinit var service: KnowledgeImportService

    private val agentId = UUID.randomUUID()
    private val knowledgeId = UUID.randomUUID()
    private lateinit var knowledge: AgentKnowledge

    @BeforeEach
    fun setUp() {
        // --------------------------------------
        // Mock Knowledge entity
        // --------------------------------------
        knowledge =
            AgentKnowledge(
                agent = mockk(relaxed = true),
                name = "Test Knowledge",
                sourceType = KnowledgeSourceType.WEB_URL,
                sourceUri = "http://example.com",
                metadata = emptyMap(),
            ).apply {
                id = knowledgeId
                active = true
                status = KnowledgeStatus.PENDING
            }

        // --------------------------------------
        // Mock services
        // --------------------------------------
        chunkService = mockk(relaxed = true)
        knowledgeRepo = mockk()

        every { knowledgeRepo.findByIdAndAgentId(knowledgeId, agentId) } returns knowledge
        every { knowledgeRepo.save(any()) } answers { firstArg() }

        every { chunkService.getNextChunkOrderForKnowledge(agentId, knowledgeId) } returns 1

        coEvery {
            chunkService.createChunkAndEnqueueEmbedding(
                agentId = any(),
                knowledgeId = any(),
                content = any(),
                metadata = any(),
                chunkOrder = any(),
            )
        } returns
            KnowledgeChunkResponseDto(
                id = UUID.randomUUID(),
                content = "mock-content",
                metadata = emptyMap(),
            )

        // --------------------------------------
        // Chunker configuration
        // --------------------------------------
        val chunkerProperties =
            ChunkerProperties().apply {
                profiles["semantic"] =
                    ChunkerProperties.ChunkerProfile().apply {
                        chunkSize = 50
                        minChunkSizeChars = 10
                        minChunkLengthToEmbed = 5
                        maxNumChunks = 200
                        keepSeparator = true
                    }
                profiles["markdown"] = profiles["semantic"]!!
                profiles["sentence"] = profiles["semantic"]!!
                profiles["code"] = profiles["semantic"]!!
            }

        documentChunker =
            DocumentChunker(
                chunkerProperties,
                DocumentTextExtractor(),
                ChunkerProfileDetector(),
            )

        service = KnowledgeImportService(chunkService, knowledgeRepo, documentChunker)
    }

    // -------------------------------------------------------
    // TESTS
    // -------------------------------------------------------

    @Test
    fun `performImport should process txt file correctly`() {
        val content = (1..20).joinToString(" ") { "Sentence $it" }

        val file = MockMultipartFile("file", "test.txt", "text/plain", content.toByteArray())

        val chunks = service.performImport(agentId, knowledgeId, file)

        assertTrue(chunks > 1)

        assertEquals(KnowledgeStatus.EMBEDDING_PENDING, knowledge.status)
        assertNull(knowledge.errorMessage)
    }

    @Test
    fun `performImport should process md file correctly`() {
        val content = (1..20).joinToString(" ") { "# H$it markdown sample text" }

        val file = MockMultipartFile("file", "test.md", "text/markdown", content.toByteArray())

        val chunks = service.performImport(agentId, knowledgeId, file)

        assertTrue(chunks > 1)

        assertEquals(KnowledgeStatus.EMBEDDING_PENDING, knowledge.status)
        assertNull(knowledge.errorMessage)
    }

    @Test
    fun `performImport should throw BadRequestException for empty file`() {
        val file = MockMultipartFile("file", "empty.txt", "text/plain", ByteArray(0))

        val ex =
            assertFailsWith<BadRequestException> {
                service.performImport(agentId, knowledgeId, file)
            }

        assert(ex.message!!.contains("contains no readable text"))
    }

    @Test
    fun `performImport should process pdf file correctly`() {
        val pdfBytes =
            ByteArrayOutputStream().use { out ->
                PDDocument().use { doc ->
                    val page = PDPage()
                    doc.addPage(page)
                    PDPageContentStream(doc, page).use { cs ->
                        cs.beginText()
                        cs.setFont(PDType1Font.HELVETICA, 12f)
                        cs.newLineAtOffset(100f, 700f)
                        cs.showText("PDF test content for chunking")
                        cs.endText()
                    }
                    doc.save(out)
                }
                out.toByteArray()
            }

        val file = MockMultipartFile("file", "test.pdf", "application/pdf", pdfBytes)

        val chunks = service.performImport(agentId, knowledgeId, file)

        assertTrue(chunks >= 1)

        assertEquals(KnowledgeStatus.EMBEDDING_PENDING, knowledge.status)
        assertNull(knowledge.errorMessage)
    }
}

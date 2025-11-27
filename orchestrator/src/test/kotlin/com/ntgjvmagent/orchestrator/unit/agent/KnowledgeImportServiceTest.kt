package com.ntgjvmagent.orchestrator.unit.agent

import com.ntgjvmagent.orchestrator.chunking.ChunkerProfileDetector
import com.ntgjvmagent.orchestrator.chunking.DocumentChunker
import com.ntgjvmagent.orchestrator.chunking.DocumentTextExtractor
import com.ntgjvmagent.orchestrator.config.ChunkerProperties
import com.ntgjvmagent.orchestrator.dto.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.dto.SystemSettingResponseDto
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import com.ntgjvmagent.orchestrator.service.SystemSettingService
import io.mockk.every
import io.mockk.mockk
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KnowledgeImportServiceTest {
    private lateinit var chunkService: KnowledgeChunkService
    private lateinit var systemSettingService: SystemSettingService
    private lateinit var knowledgeRepo: AgentKnowledgeRepository
    private lateinit var documentChunker: DocumentChunker
    private lateinit var service: KnowledgeImportService

    private val agentId = UUID.randomUUID()
    private val knowledgeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        chunkService = mockk()
        systemSettingService = mockk()
        knowledgeRepo = mockk()

        // Always return true for knowledge existence
        every { knowledgeRepo.existsByIdAndAgentId(any(), any()) } returns true

        // Setup chunk order mock
        every { chunkService.getNextChunkOrderForKnowledge(agentId, knowledgeId) } returns 0

        every { systemSettingService.getSystemSetting() } returns SystemSettingResponseDto(
            id = UUID.randomUUID(),
            siteName = "TEST",
            maximumUser = 10,
            sessionTimeout = 10,
            maximumSizeFileUpload = 10,
            allowedFileTypes = "txt, pdf, docx, md",
            maintenanceMode = true,
            userRegistration = true,
            emailVerification = true,
            )

        // Correctly map arguments (NO MORE CLASSCAST ISSUES)
        every {
            chunkService.addChunk(
                agentId = any(),
                knowledgeId = any(),
                chunkOrder = any(),
                content = any(),
                metadata = any(),
            )
        } answers {
            KnowledgeChunkResponseDto(
                id = UUID.randomUUID(),
                content = arg<String>(2),
                metadata = arg<Map<String, Any>?>(3),
            )
        }

        // Chunker with simple deterministic profiles
        val chunkerProperties =
            ChunkerProperties().apply {
                profiles["default"] =
                    ChunkerProperties.ChunkerProfile().apply {
                        chunkSize = 50
                        minChunkSizeChars = 10
                        minChunkLengthToEmbed = 5
                        maxNumChunks = 200
                        keepSeparator = true
                    }
                profiles["markdown"] = profiles["default"]!!
                profiles["loose"] = profiles["default"]!!
                profiles["tight"] = profiles["default"]!!
                profiles["code"] = profiles["default"]!!
            }

        documentChunker =
            DocumentChunker(
                chunkerProperties,
                DocumentTextExtractor(),
                ChunkerProfileDetector(),
            )

        service = KnowledgeImportService(chunkService, knowledgeRepo, documentChunker, systemSettingService)
    }

    // -------------------------------------------------------
    // TESTS
    // -------------------------------------------------------

    @Test
    fun `importDocument should process txt file correctly`() {
        val content = (1..20).joinToString(" ") { "Sentence $it" }

        val file =
            MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                content.toByteArray(),
            )

        val response = service.importDocument(agentId, knowledgeId, file)

        assertEquals("test.txt", response.originalFilename)
        assertTrue(response.numberOfSegment > 1)
    }

    @Test
    fun `importDocument should process md file correctly`() {
        val content = (1..20).joinToString(" ") { "# Heading $it some markdown text." }

        val file =
            MockMultipartFile(
                "file",
                "test.md",
                "text/markdown",
                content.toByteArray(),
            )

        val response = service.importDocument(agentId, knowledgeId, file)

        assertEquals("test.md", response.originalFilename)
        assertTrue(response.numberOfSegment > 1)
    }

    @Test
    fun `importDocument should throw BadRequestException for empty file`() {
        val file =
            MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                ByteArray(0),
            )

        val exception =
            assertFailsWith<BadRequestException> {
                service.importDocument(agentId, knowledgeId, file)
            }

        assert(exception.message!!.contains("empty or contains no readable text"))
    }

    @Test
    fun `importDocument should process pdf file correctly`() {
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

        val file =
            MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                pdfBytes,
            )

        val response = service.importDocument(agentId, knowledgeId, file)

        assertEquals("test.pdf", response.originalFilename)
        assertTrue(response.numberOfSegment >= 1)
    }
}

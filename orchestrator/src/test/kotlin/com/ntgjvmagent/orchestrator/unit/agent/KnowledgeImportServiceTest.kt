package com.ntgjvmagent.orchestrator.unit.agent

import com.ntgjvmagent.orchestrator.chunking.ChunkerProfileDetector
import com.ntgjvmagent.orchestrator.chunking.DocumentChunker
import com.ntgjvmagent.orchestrator.chunking.DocumentTextExtractor
import com.ntgjvmagent.orchestrator.config.ChunkerProperties
import com.ntgjvmagent.orchestrator.dto.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
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
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KnowledgeImportServiceTest {
    private lateinit var chunkService: KnowledgeChunkService
    private lateinit var documentChunker: DocumentChunker
    private lateinit var service: KnowledgeImportService

    @BeforeEach
    fun setUp() {
        chunkService = mockk()

        // DocumentChunker with small chunk sizes for deterministic multi-chunk output
        val chunkerProperties =
            ChunkerProperties().apply {
                profiles["default"] =
                    ChunkerProperties.ChunkerProfile().apply {
                        chunkSize = 50
                        minChunkSizeChars = 10
                        minChunkLengthToEmbed = 5
                        maxNumChunks = 100
                        keepSeparator = true
                    }
                profiles["markdown"] =
                    ChunkerProperties.ChunkerProfile().apply {
                        chunkSize = 50
                        minChunkSizeChars = 10
                        minChunkLengthToEmbed = 5
                        maxNumChunks = 100
                        keepSeparator = true
                    }
                profiles["loose"] =
                    ChunkerProperties.ChunkerProfile().apply {
                        chunkSize = 50
                        minChunkSizeChars = 10
                        minChunkLengthToEmbed = 5
                        maxNumChunks = 100
                        keepSeparator = true
                    }
                profiles["tight"] =
                    ChunkerProperties.ChunkerProfile().apply {
                        chunkSize = 30
                        minChunkSizeChars = 10
                        minChunkLengthToEmbed = 5
                        maxNumChunks = 200
                        keepSeparator = true
                    }
                profiles["code"] =
                    ChunkerProperties.ChunkerProfile().apply {
                        chunkSize = 50
                        minChunkSizeChars = 10
                        minChunkLengthToEmbed = 5
                        maxNumChunks = 500
                        keepSeparator = true
                    }
            }
        val textExtractor = DocumentTextExtractor()
        val profileDetector = ChunkerProfileDetector()
        documentChunker = DocumentChunker(chunkerProperties, textExtractor, profileDetector)

        service = KnowledgeImportService(chunkService, documentChunker)

        // Mock chunkService methods
        every { chunkService.getNextChunkOrderForKnowledge(any()) } returns 0
        every { chunkService.addChunk(any(), any(), any(), any()) } answers {
            KnowledgeChunkResponseDto(
                id = UUID.randomUUID(),
                content = secondArg(),
                metadata = thirdArg(),
            )
        }
    }

    @Test
    fun `importDocument should process txt file correctly`() {
        val content = (1..20).joinToString(" ") { "Sentence $it" } // deterministic multi-chunk text
        val file =
            MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                content.toByteArray(StandardCharsets.UTF_8),
            )

        val response = service.importDocument(UUID.randomUUID(), file)

        assertEquals("test.txt", response.originalFilename)
        assertTrue(response.numberOfSegment > 1, "Expected multiple chunks for txt file")
    }

    @Test
    fun `importDocument should process md file correctly`() {
        val content = (1..20).joinToString(" ") { "# Heading $it Some markdown text." }
        val file =
            MockMultipartFile(
                "file",
                "test.md",
                "text/markdown",
                content.toByteArray(StandardCharsets.UTF_8),
            )

        val response = service.importDocument(UUID.randomUUID(), file)

        assertEquals("test.md", response.originalFilename)
        assertTrue(response.numberOfSegment > 1, "Expected multiple chunks for md file")
    }

    @Test
    fun `importDocument should throw BadRequestException for empty file`() {
        val file =
            MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                "".toByteArray(StandardCharsets.UTF_8),
            )

        val exception =
            assertFailsWith<BadRequestException> {
                service.importDocument(UUID.randomUUID(), file)
            }
        assert(exception.message!!.contains("empty or contains no readable text"))
    }

    @Test
    fun `importDocument should process pdf file correctly`() {
        val pdfBytes =
            ByteArrayOutputStream().use { out ->
                val document = PDDocument()
                val page = PDPage()
                document.addPage(page)
                val contentStream = PDPageContentStream(document, page)
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA, 12f)
                contentStream.newLineAtOffset(100f, 700f)
                contentStream.showText("PDF test content for chunking with multiple lines.")
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

        val response = service.importDocument(UUID.randomUUID(), file)

        assertEquals("test.pdf", response.originalFilename)
        assertTrue(response.numberOfSegment >= 1, "Expected at least one chunk for PDF")
    }
}

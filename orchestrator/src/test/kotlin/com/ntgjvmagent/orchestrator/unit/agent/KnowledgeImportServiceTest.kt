package com.ntgjvmagent.orchestrator.unit.agent

import com.ntgjvmagent.orchestrator.dto.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import com.ntgjvmagent.orchestrator.viewmodel.KnowledgeImportingResponseVm
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KnowledgeImportServiceTest {
    private lateinit var chunkService: KnowledgeChunkService
    private lateinit var service: KnowledgeImportService

    @BeforeEach
    fun setUp() {
        chunkService = mockk()
        service = KnowledgeImportService(chunkService)

        // Mock addChunk to return dummy KnowledgeChunkResponseDto
        every { chunkService.addChunk(any(), any(), any()) } answers {
            KnowledgeChunkResponseDto(
                id = UUID.randomUUID(),
                content = secondArg(),
                metadata = thirdArg(),
            )
        }
    }

    @Test
    fun `importDocument should process txt file correctly`() {
        val content = "This is a test document content."
        val file =
            MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                content.toByteArray(StandardCharsets.UTF_8),
            )

        val response: KnowledgeImportingResponseVm =
            service.importDocument(UUID.randomUUID(), file)

        assertEquals("test.txt", response.originalFilename)
        assertEquals(1, response.numberOfSegment)
    }

    @Test
    fun `importDocument should process md file correctly`() {
        val content = "# Markdown Title\nSome text here."
        val file =
            MockMultipartFile(
                "file",
                "test.md",
                "text/markdown",
                content.toByteArray(StandardCharsets.UTF_8),
            )

        val response = service.importDocument(UUID.randomUUID(), file)

        assertEquals("test.md", response.originalFilename)
        assertEquals(1, response.numberOfSegment)
    }

    @Test
    fun `importDocument should throw BadRequestException for unsupported file`() {
        val file =
            MockMultipartFile(
                "file",
                "unsupported.exe",
                "application/octet-stream",
                byteArrayOf(0x00),
            )

        val exception =
            assertFailsWith<BadRequestException> {
                service.importDocument(UUID.randomUUID(), file)
            }
        assert(exception.message!!.contains("Only support"))
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
    fun `importText should create chunks and return response`() {
        val text = "Some sample text for direct importText."
        val response = service.importText(UUID.randomUUID(), "sample.txt", text)

        assertEquals("sample.txt", response.originalFilename)
        assertEquals(1, response.numberOfSegment)
    }

    @Test
    fun `importDocument should process pdf file correctly`() {
        // Arrange
        val pdfBytes = ByteArray(10) { 0 }
        val file =
            MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                pdfBytes,
            )

        val serviceSpy = spyk(service, recordPrivateCalls = true)

        // Mock the private method extractTextFromPdf(file)
        every { serviceSpy invoke "extractTextFromPdf" withArguments listOf(file) } returns "Hello PDF text content"

        // Mock addChunk()
        every {
            chunkService.addChunk(any(), any(), any())
        } returns KnowledgeChunkResponseDto(UUID.randomUUID(), "Chunk 1", mapOf("source" to "test.pdf"))

        // Act
        val response = serviceSpy.importDocument(UUID.randomUUID(), file)

        // Assert
        assertEquals("test.pdf", response.originalFilename)
        assertEquals(1, response.numberOfSegment)
    }
}

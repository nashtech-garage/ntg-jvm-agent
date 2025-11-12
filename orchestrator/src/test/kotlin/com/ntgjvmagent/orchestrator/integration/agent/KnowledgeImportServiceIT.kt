package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.integration.config.TestEmbeddingConfig
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
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
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Import(TestEmbeddingConfig::class)
class KnowledgeImportServiceIT
    @Autowired
    constructor(
        private val service: KnowledgeImportService,
        private val knowledgeRepo: AgentKnowledgeRepository,
        private val chunkRepo: KnowledgeChunkRepository,
    ) : BaseIntegrationTest() {
        private lateinit var knowledge: AgentKnowledge

        @BeforeEach
        fun setUp() {
            chunkRepo.deleteAll()
            knowledgeRepo.deleteAll()
            knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        name = "Test Knowledge",
                        sourceType = "manual",
                        sourceUri = "http://example.com",
                        metadata = mapOf(),
                        embeddingModel = "text-embedding-3-small",
                    ).apply { active = true },
                )
        }

        @Test
        fun `importDocument should save txt file chunks to DB`() {
            val content = "This is a simple text document for testing."
            val file =
                MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    content.toByteArray(StandardCharsets.UTF_8),
                )

            val response: KnowledgeImportingResponseVm =
                service.importDocument(knowledge.id!!, file)

            assertEquals("test.txt", response.originalFilename)
            assertTrue(response.numberOfSegment > 0)

            // Verify chunks saved in DB
            val savedChunks = chunkRepo.findAllByKnowledgeId(knowledge.id!!)
            assertEquals(response.numberOfSegment, savedChunks.size)
            assertTrue(savedChunks.any { it.content.contains("simple text") })
        }

        @Test
        fun `importDocument should save pdf file chunks to DB`() {
            // Create in-memory PDF
            val pdfBytes =
                ByteArrayOutputStream().use { out ->
                    val document = PDDocument()
                    val page = PDPage()
                    document.addPage(page)
                    val contentStream = PDPageContentStream(document, page)
                    contentStream.beginText()
                    contentStream.setFont(PDType1Font.HELVETICA, 12f)
                    contentStream.newLineAtOffset(100f, 700f)
                    contentStream.showText("PDF test content")
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

            val response = service.importDocument(knowledge.id!!, file)

            assertEquals("test.pdf", response.originalFilename)
            assertTrue(response.numberOfSegment > 0)

            // Verify chunks saved in DB
            val savedChunks = chunkRepo.findAllByKnowledgeId(knowledge.id!!)
            assertEquals(response.numberOfSegment, savedChunks.size)
            assertTrue(savedChunks.any { it.content.contains("PDF test content") })
        }

        @Test
        fun `importText should save chunks to DB`() {
            val text = "Some sample text for direct importText."
            val response = service.importText(knowledge.id!!, "sample.txt", text)

            assertEquals("sample.txt", response.originalFilename)
            assertTrue(response.numberOfSegment > 0)

            val savedChunks = chunkRepo.findAllByKnowledgeId(knowledge.id!!)
            assertEquals(response.numberOfSegment, savedChunks.size)
            assertTrue(savedChunks.any { it.content.contains("sample text") })
        }
    }

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
import com.ntgjvmagent.orchestrator.storage.core.ObjectStorage
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KnowledgeImportServiceTest {
    private lateinit var chunkService: KnowledgeChunkService
    private lateinit var knowledgeRepo: AgentKnowledgeRepository
    private lateinit var objectStorage: ObjectStorage
    private lateinit var documentChunker: DocumentChunker
    private lateinit var service: KnowledgeImportService

    private val agentId = UUID.randomUUID()
    private val knowledgeId = UUID.randomUUID()

    private lateinit var knowledge: AgentKnowledge

    @BeforeEach
    fun setUp() {
        knowledge =
            AgentKnowledge(
                agent = mockk(relaxed = true),
                name = "Test Knowledge",
                sourceType = KnowledgeSourceType.FILE,
                sourceUri = null,
                originalFileName = "test.txt",
                storageKey = "agents/$agentId/knowledge/$knowledgeId/original",
                metadata = emptyMap(),
            ).apply {
                id = knowledgeId
                active = true
                status = KnowledgeStatus.PENDING
            }

        chunkService = mockk(relaxed = true)
        knowledgeRepo = mockk()
        objectStorage = mockk()

        every { knowledgeRepo.findByIdAndAgentId(knowledgeId, agentId) } returns knowledge
        every { knowledgeRepo.save(any()) } answers { firstArg() }
        every { objectStorage.exists(knowledge.storageKey!!) } returns true

        every { chunkService.getNextChunkOrderForKnowledge(agentId, knowledgeId) } returns 1

        every {
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
                content = "mock",
                metadata = emptyMap(),
            )

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

        service =
            KnowledgeImportService(
                chunkService,
                knowledgeRepo,
                documentChunker,
                objectStorage,
            )
    }

    // -------------------------------------------------------
    // TESTS
    // -------------------------------------------------------

    @Test
    fun `performImport should process txt file correctly`() {
        val content = (1..20).joinToString(" ") { "Sentence $it" }

        every {
            objectStorage.load(knowledge.storageKey!!)
        } returns ByteArrayInputStream(content.toByteArray())

        val chunks = service.performImport(agentId, knowledgeId)

        assertTrue(chunks > 1)
        assertEquals(KnowledgeStatus.EMBEDDING_PENDING, knowledge.status)
        assertNull(knowledge.errorMessage)
    }

    @Test
    fun `performImport should process markdown file correctly`() {
        val content = (1..20).joinToString(" ") { "# H$it markdown sample text" }

        knowledge.originalFileName = "test.md"

        every {
            objectStorage.load(knowledge.storageKey!!)
        } returns ByteArrayInputStream(content.toByteArray())

        val chunks = service.performImport(agentId, knowledgeId)

        assertTrue(chunks > 1)
        assertEquals(KnowledgeStatus.EMBEDDING_PENDING, knowledge.status)
    }

    @Test
    fun `performImport should throw for empty file`() {
        every {
            objectStorage.load(knowledge.storageKey!!)
        } returns ByteArrayInputStream(ByteArray(0))

        val ex =
            assertFailsWith<BadRequestException> {
                service.performImport(agentId, knowledgeId)
            }

        assertTrue(ex.message!!.contains("contains no readable text"))
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

        knowledge.originalFileName = "test.pdf"

        every {
            objectStorage.load(knowledge.storageKey!!)
        } returns ByteArrayInputStream(pdfBytes)

        val chunks = service.performImport(agentId, knowledgeId)

        assertTrue(chunks >= 1)
        assertEquals(KnowledgeStatus.EMBEDDING_PENDING, knowledge.status)
    }
}

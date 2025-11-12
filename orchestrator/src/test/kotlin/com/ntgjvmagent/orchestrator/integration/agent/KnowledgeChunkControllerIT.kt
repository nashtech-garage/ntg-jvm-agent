package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.KnowledgeChunkRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.integration.config.TestEmbeddingConfig
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayOutputStream
import kotlin.test.Test

@Import(TestEmbeddingConfig::class)
class KnowledgeChunkControllerIT
    @Autowired
    constructor(
        private val chunkRepo: KnowledgeChunkRepository,
        private val knowledgeRepo: AgentKnowledgeRepository,
    ) : BaseIntegrationTest() {
        private lateinit var knowledge: AgentKnowledge

        @BeforeEach
        fun setup() {
            chunkRepo.deleteAllInBatch()
            knowledgeRepo.deleteAllInBatch()

            knowledge = knowledgeRepo.save(AgentKnowledge(name = "K1").apply { active = true })
        }

        private fun createKnowledgeChunkRequest(content: String = "Test content") =
            KnowledgeChunkRequestDto(
                content = content,
                metadata = mapOf("source" to "unit-test"),
            )

        @Test
        fun `createChunk endpoint should return 201 and persist embedding`() {
            val request = KnowledgeChunkRequestDto("Test chunk", emptyMap())

            mockMvc
                .perform(
                    postAuth("/api/knowledge/${knowledge.id}/chunks", request, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isCreated)

            // Verify that the chunk was persisted
            val persisted = chunkRepo.findAll()
            assertEquals(1, persisted.size)
            assertEquals("Test chunk", persisted[0].content)
            assertEquals(1536, persisted[0].embedding.size) // ensure embedding dimension matches PgVector
        }

        @Test
        fun `full KnowledgeChunkController flow with txt and pdf imports`() {
            val knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        name = "Test Knowledge",
                        sourceType = "pdf",
                        sourceUri = "unit-test://file.pdf",
                        metadata = mapOf("createdBy" to "integration-test"),
                        embeddingModel = "mock-embedding",
                    ),
                )
            val knowledgeId = knowledge.id!!

            // ------------------ CREATE CHUNK ------------------
            val createReq = createKnowledgeChunkRequest("Hello World")

            val chunkId =
                mockMvc
                    .perform(
                        postAuth("/api/knowledge/$knowledgeId/chunks", createReq, roles = listOf("ROLE_ADMIN")),
                    ).andExpect(status().isCreated)
                    .andExpect(jsonPath("$.content").value("Hello World"))
                    .andExpect(jsonPath("$.metadata.source").value("unit-test"))
                    .andReturn()
                    .response
                    .let { objectMapper.readTree(it.contentAsString).get("id").asText() }

            // ------------------ UPDATE CHUNK ------------------
            val updateReq = createKnowledgeChunkRequest("Updated content")

            mockMvc
                .perform(
                    putAuth("/api/knowledge/$knowledgeId/chunks/$chunkId", updateReq, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.content").value("Updated content"))

            // ------------------ LIST CHUNKS ------------------
            mockMvc
                .perform(getAuth("/api/knowledge/$knowledgeId/chunks", roles = listOf("ROLE_ADMIN")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))

            // ------------------ COUNT CHUNKS ------------------
            mockMvc
                .perform(getAuth("/api/knowledge/$knowledgeId/chunks/count", roles = listOf("ROLE_ADMIN")))
                .andExpect(status().isOk)
                .andExpect(content().string("1"))

            // ------------------ IMPORT TXT FILE ------------------
            val txtFile =
                MockMultipartFile(
                    "file",
                    "test.txt",
                    MediaType.TEXT_PLAIN_VALUE,
                    "This is a simple text file for testing import.".toByteArray(),
                )

            mockMvc
                .perform(
                    multipartAuth("/api/knowledge/$knowledgeId/chunks/import", txtFile, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.originalFilename").value("test.txt"))
                .andExpect(jsonPath("$.numberOfSegment").isNumber)

            // ------------------ IMPORT PDF FILE ------------------
            val pdfBytes =
                ByteArrayOutputStream().use { out ->
                    PDDocument().use { doc ->
                        val page = PDPage()
                        doc.addPage(page)
                        PDPageContentStream(doc, page).use { cs ->
                            cs.beginText()
                            cs.setFont(PDType1Font.HELVETICA, 12f)
                            cs.newLineAtOffset(50f, 700f)
                            cs.showText("PDF test content for multiple chunks")
                            cs.endText()
                        }
                        doc.save(out)
                    }
                    out.toByteArray()
                }

            val pdfFile =
                MockMultipartFile(
                    "file",
                    "test.pdf",
                    "application/pdf",
                    pdfBytes,
                )

            val pdfImportResult =
                mockMvc
                    .perform(
                        multipartAuth(
                            "/api/knowledge/$knowledgeId/chunks/import",
                            pdfFile,
                            roles = listOf("ROLE_ADMIN"),
                        ),
                    ).andExpect(status().isCreated)
                    .andExpect(jsonPath("$.originalFilename").value("test.pdf"))
                    .andExpect(jsonPath("$.numberOfSegment").isNumber)
                    .andReturn()
                    .response
                    .let { objectMapper.readTree(it.contentAsString).get("numberOfSegment").asInt() }

            // Ensure multiple chunks created
            assert(pdfImportResult > 0)

            // ------------------ SEARCH CHUNKS ------------------
            mockMvc
                .perform(
                    getAuth("/api/knowledge/$knowledgeId/chunks/search", roles = listOf("ROLE_ADMIN"))
                        .param("query", "PDF test")
                        .param("topK", "5"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").isNumber)
        }
    }

package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.config.VectorEmbeddingProperties
import com.ntgjvmagent.orchestrator.dto.KnowledgeChunkRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.integration.config.TestEmbeddingConfig
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        private val agentRepo: AgentRepository,
        private val properties: VectorEmbeddingProperties,
    ) : BaseIntegrationTest() {
        private lateinit var agent: Agent
        private lateinit var knowledge: AgentKnowledge

        @BeforeEach
        fun setup() {
            chunkRepo.deleteAllInBatch()
            knowledgeRepo.deleteAllInBatch()
            agentRepo.deleteAllInBatch()
            agentRepo.flush()
            knowledgeRepo.flush()
            chunkRepo.flush()

            agent =
                agentRepo.save(
                    Agent(
                        name = "Test Agent",
                        model = "gpt-4",
                    ),
                )

            knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "K1",
                    ).apply { active = true },
                )
        }

        private fun createKnowledgeChunkRequest(content: String = "Test content") =
            KnowledgeChunkRequestDto(
                content = content,
                metadata = mapOf("source" to "unit-test"),
            )

        @Test
        fun `createChunk endpoint should return 201 and persist embedding`() {
            val request = createKnowledgeChunkRequest("Test chunk")

            mockMvc
                .perform(
                    postAuth(
                        "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks",
                        request,
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isCreated)

            val persisted = chunkRepo.findAll()
            assertEquals(1, persisted.size)
            assertEquals("Test chunk", persisted[0].content)
            assertEquals(properties.embeddingDimension, persisted[0].embedding.size)
        }

        @Test
        fun `full KnowledgeChunkController flow with txt and pdf imports`() {
            val knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        agent = agent,
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
                        postAuth(
                            "/api/agents/${agent.id}/knowledge/$knowledgeId/chunks",
                            createReq,
                            roles = listOf("ROLE_ADMIN"),
                        ),
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
                    putAuth(
                        "/api/agents/${agent.id}/knowledge/$knowledgeId/chunks/$chunkId",
                        updateReq,
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.content").value("Updated content"))

            // ------------------ LIST CHUNKS ------------------
            mockMvc
                .perform(getAuth("/api/agents/${agent.id}/knowledge/$knowledgeId/chunks", roles = listOf("ROLE_ADMIN")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))

            // ------------------ COUNT CHUNKS ------------------
            mockMvc
                .perform(
                    getAuth(
                        "/api/agents/${agent.id}/knowledge/$knowledgeId/chunks/count",
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
                .andExpect(content().string("1"))

            // ------------------ IMPORT TXT FILE ------------------
            val longTxtContent =
                buildString {
                    repeat(10) { append("This is sentence $it for testing chunking. ") }
                }
            val txtFile =
                MockMultipartFile(
                    "file",
                    "test.txt",
                    MediaType.TEXT_PLAIN_VALUE,
                    longTxtContent.toByteArray(),
                )

            val txtResponse =
                mockMvc
                    .perform(
                        multipartAuth(
                            "/api/agents/${agent.id}/knowledge/$knowledgeId/chunks/import",
                            txtFile,
                            roles = listOf("ROLE_ADMIN"),
                        ),
                    ).andExpect(status().isCreated)
                    .andExpect(jsonPath("$.originalFilename").value("test.txt"))
                    .andExpect(jsonPath("$.numberOfSegment").isNumber)
                    .andReturn()
                    .response

            val txtChunksCount =
                objectMapper.readTree(txtResponse.contentAsString).get("numberOfSegment").asInt()
            assertTrue(txtChunksCount >= 1, "Expected at least one chunk for txt")

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
                            repeat(10) { cs.showText("PDF test content sentence $it. ") }
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

            val pdfResponse =
                mockMvc
                    .perform(
                        multipartAuth(
                            "/api/agents/${agent.id}/knowledge/$knowledgeId/chunks/import",
                            pdfFile,
                            roles = listOf("ROLE_ADMIN"),
                        ),
                    ).andExpect(status().isCreated)
                    .andExpect(jsonPath("$.originalFilename").value("test.pdf"))
                    .andExpect(jsonPath("$.numberOfSegment").isNumber)
                    .andReturn()
                    .response

            val pdfChunksCount =
                objectMapper.readTree(pdfResponse.contentAsString).get("numberOfSegment").asInt()
            assertTrue(pdfChunksCount >= 1, "Expected at least one chunk for PDF")

            // ------------------ SEARCH CHUNKS ------------------
            mockMvc
                .perform(
                    getAuth(
                        "/api/agents/${agent.id}/knowledge/$knowledgeId/chunks/search",
                        roles = listOf("ROLE_ADMIN"),
                    ).param("query", "PDF test")
                        .param("topK", "5"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").isNumber)
        }
    }

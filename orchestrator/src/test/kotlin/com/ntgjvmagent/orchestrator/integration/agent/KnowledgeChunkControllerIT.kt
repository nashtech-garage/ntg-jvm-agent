package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.request.KnowledgeChunkRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
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
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import kotlin.test.Test

@Transactional
class KnowledgeChunkControllerIT
    @Autowired
    constructor(
        private val chunkRepo: KnowledgeChunkRepository,
        private val knowledgeRepo: AgentKnowledgeRepository,
        private val agentRepo: AgentRepository,
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
                        provider = "OpenAI",
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        model = "gpt-4",
                        embeddingModel = "openai/text-embedding-3-small",
                        embeddingsPath = "/embeddings",
                    ),
                )

            // Must specify sourceType and metadata (defaults are emptyMap)
            knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "K1",
                        sourceType = KnowledgeSourceType.INLINE,
                        sourceUri = null,
                        metadata = emptyMap(),
                    ).apply { active = true },
                )
        }

        private fun createChunkReq(content: String = "Test content") =
            KnowledgeChunkRequestDto(
                content = content,
                metadata = mapOf("source" to "unit-test"),
            )

        // ----------------------------------------------------------------------
        // CREATE CHUNK
        // ----------------------------------------------------------------------
        @Test
        fun `createChunk endpoint should return 201 and persist embedding`() {
            val req = createChunkReq("Test chunk")

            mockMvc
                .perform(
                    postAuth(
                        "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks",
                        req,
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.content").value("Test chunk"))
                .andExpect(jsonPath("$.metadata.source").value("unit-test"))

            val persisted = chunkRepo.findAll()
            assertEquals(1, persisted.size)
            assertEquals("Test chunk", persisted[0].content)
        }

        // ----------------------------------------------------------------------
        // FULL FLOW
        // ----------------------------------------------------------------------
        @Test
        fun `full KnowledgeChunkController flow with txt and pdf imports`() {
            // Must specify proper fields for FILE knowledge
            val knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Test Knowledge",
                        sourceType = KnowledgeSourceType.FILE,
                        sourceUri = "unit-test://file.pdf",
                        metadata = mapOf("createdBy" to "integration-test"),
                    ).apply { active = true },
                )

            val knowledgeId = knowledge.id!!

            // ------------------ CREATE CHUNK ------------------
            val createReq = createChunkReq("Hello World")
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
            val updateReq = createChunkReq("Updated content")
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
                .perform(
                    getAuth(
                        "/api/agents/${agent.id}/knowledge/$knowledgeId/chunks",
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
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
            val longText =
                buildString {
                    repeat(10) { append("This is sentence $it for testing chunking. ") }
                }

            val txtFile =
                MockMultipartFile(
                    "file", // MUST match @RequestPart("file")
                    "test.txt",
                    MediaType.TEXT_PLAIN_VALUE,
                    longText.toByteArray(),
                )

            val txtResp =
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

            val txtChunkCount =
                objectMapper.readTree(txtResp.contentAsString).get("numberOfSegment").asInt()
            assertTrue(txtChunkCount >= 1)

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

            val pdfResp =
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

            val pdfChunkCount =
                objectMapper.readTree(pdfResp.contentAsString).get("numberOfSegment").asInt()
            assertTrue(pdfChunkCount >= 1)

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

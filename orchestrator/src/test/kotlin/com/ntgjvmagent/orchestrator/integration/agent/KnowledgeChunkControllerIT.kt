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
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test

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

        @Test
        fun `full KnowledgeChunkController flow with txt and pdf imports`() {
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

            // ------------------ CREATE CHUNK ------------------
            val chunkId =
                mockMvc
                    .perform(
                        postAuth(
                            "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks",
                            createChunkReq("Hello World"),
                            roles = listOf("ROLE_ADMIN"),
                        ),
                    ).andExpect(status().isCreated)
                    .andExpect(jsonPath("$.content").value("Hello World"))
                    .andExpect(jsonPath("$.metadata.source").value("unit-test"))
                    .andReturn()
                    .response
                    .let { objectMapper.readTree(it.contentAsString).get("id").asText() }

            // ------------------ UPDATE CHUNK ------------------
            mockMvc
                .perform(
                    putAuth(
                        "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks/$chunkId",
                        createChunkReq("Updated content"),
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.content").value("Updated content"))

            // ------------------ LIST + COUNT ------------------
            mockMvc
                .perform(
                    getAuth(
                        "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks",
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))

            mockMvc
                .perform(
                    getAuth(
                        "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks/count",
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
                .andExpect(content().string("1"))

            // ------------------ IMPORT TXT ------------------
            val txtFile =
                MockMultipartFile(
                    "file",
                    "test.txt",
                    MediaType.TEXT_PLAIN_VALUE,
                    "Sentence 1 Sentence 2 Sentence 3".toByteArray(StandardCharsets.UTF_8),
                )

            mockMvc
                .perform(
                    multipartAuth(
                        "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks/import",
                        txtFile,
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isAccepted)
                .andExpect(jsonPath("$.originalFilename").value("test.txt"))

            // ------------------ IMPORT PDF ------------------
            val pdfBytes =
                ByteArrayOutputStream().use { out ->
                    PDDocument().use { doc ->
                        val page = PDPage()
                        doc.addPage(page)
                        PDPageContentStream(doc, page).use { cs ->
                            cs.beginText()
                            cs.setFont(PDType1Font.HELVETICA, 12f)
                            cs.newLineAtOffset(50f, 700f)
                            cs.showText("PDF test content.")
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

            mockMvc
                .perform(
                    multipartAuth(
                        "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks/import",
                        pdfFile,
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isAccepted)
                .andExpect(jsonPath("$.originalFilename").value("test.pdf"))

            // ------------------ SEARCH ------------------
            mockMvc
                .perform(
                    getAuth(
                        "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks/search",
                        roles = listOf("ROLE_ADMIN"),
                    ).param("query", "Hello")
                        .param("topK", "5"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").isNumber)
        }
    }

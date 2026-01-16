package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.request.KnowledgeChunkRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeService
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayOutputStream
import kotlin.test.Test

class KnowledgeChunkControllerIT
    @Autowired
    constructor(
        private val chunkRepo: KnowledgeChunkRepository,
        private val knowledgeRepo: AgentKnowledgeRepository,
        private val agentRepo: AgentRepository,
        private val agentKnowledgeService: AgentKnowledgeService,
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
                        provider = ProviderType.OPENAI,
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        model = "gpt-4",
                        embeddingModel = "openai/text-embedding-3-small",
                        embeddingsPath = "/embeddings",
                    ),
                )

            // Default INLINE knowledge for first test
            knowledge =
                knowledgeRepo.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Inline K",
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

        // ------------------------------------------------------------
        // INLINE knowledge flow
        // ------------------------------------------------------------

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

        // ------------------------------------------------------------
        // FILE knowledge full lifecycle
        // ------------------------------------------------------------

        @Test
        fun `full KnowledgeChunkController flow with txt and pdf imports`() {
            // ---------------- CREATE FILE KNOWLEDGE ----------------

            val pdfBytes =
                ByteArrayOutputStream().use { out ->
                    PDDocument().use { doc ->
                        val page = PDPage()
                        doc.addPage(page)
                        PDPageContentStream(doc, page).use {
                            it.beginText()
                            it.setFont(PDType1Font.HELVETICA, 12f)
                            it.newLineAtOffset(100f, 700f)
                            it.showText("PDF test content")
                            it.endText()
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

            val created =
                agentKnowledgeService.createFileKnowledge(
                    agentId = agent.id!!,
                    originalFileName = "test.pdf",
                    metadata = mapOf("createdBy" to "integration-test"),
                    file = file,
                )

            val knowledge = knowledgeRepo.findById(created.id).get()

            // ---------------- CREATE CHUNK ----------------

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
                    .andReturn()
                    .response
                    .let { objectMapper.readTree(it.contentAsString).get("id").asText() }

            // ---------------- UPDATE CHUNK ----------------

            mockMvc
                .perform(
                    putAuth(
                        "/api/agents/${agent.id}/knowledge/${knowledge.id}/chunks/$chunkId",
                        createChunkReq("Updated content"),
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.content").value("Updated content"))

            // ---------------- LIST + COUNT ----------------

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

            // ---------------- SEARCH ----------------

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

package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.support.SoftDeleteAssertions.assertSoftDeleted
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AgentKnowledgeControllerIT
    @Autowired
    constructor(
        private val repository: AgentKnowledgeRepository,
        private val agentRepository: AgentRepository,
        private val entityManager: EntityManager,
    ) : BaseIntegrationTest() {
        private lateinit var agent: Agent

        @BeforeEach
        fun setup() {
            repository.deleteAll()
            agentRepository.deleteAll()

            agent =
                agentRepository.save(
                    Agent(
                        name = "Test Agent",
                        model = "gpt-4",
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        embeddingsPath = "/embeddings",
                        embeddingModel = "openai/text-embedding-3-small",
                    ),
                )
        }

        @Test
        fun `should create a new agent knowledge`() {
            val req =
                AgentKnowledgeRequestDto(
                    name = "Ophthalmology Dataset",
                    sourceType = "URL",
                    sourceUri = "https://example.com/ophthalmology",
                    metadata = mapOf("category" to "medical", "language" to "en"),
                    embeddingModel = "text-embedding-3-small",
                    active = true,
                )

            mockMvc
                .perform(
                    postAuth("/api/agents/${agent.id}/knowledge", req, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("Ophthalmology Dataset"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.embeddingModel").value("text-embedding-3-small"))
        }

        @Test
        fun `should get all active knowledge for agent`() {
            repository.save(
                AgentKnowledge(
                    agent = agent,
                    name = "Knowledge A",
                    sourceType = "URL",
                    sourceUri = "https://a.com",
                ).apply { active = true },
            )
            repository.save(
                AgentKnowledge(
                    agent = agent,
                    name = "Knowledge B",
                    sourceType = "URL",
                    sourceUri = "https://b.com",
                ).apply { active = false },
            )

            mockMvc
                .perform(
                    getAuth("/api/agents/${agent.id}/knowledge", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$[0].name").value("Knowledge A"))
                .andExpect(jsonPath("$[0].active").value(true))
        }

        @Test
        fun `should get knowledge by id for agent`() {
            val entity =
                repository.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Knowledge X",
                        sourceType = "URL",
                        sourceUri = "https://example.com/x",
                        embeddingModel = "text-embedding-3-large",
                    ).apply { active = true },
                )

            mockMvc
                .perform(
                    getAuth("/api/agents/${agent.id}/knowledge/${entity.id}", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(entity.id.toString()))
                .andExpect(jsonPath("$.name").value("Knowledge X"))
                .andExpect(jsonPath("$.embeddingModel").value("text-embedding-3-large"))
        }

        @Test
        fun `should update existing knowledge for agent`() {
            val entity =
                repository.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Old Knowledge",
                        sourceType = "URL",
                        sourceUri = "https://old.com",
                        embeddingModel = "old-embed",
                    ).apply { active = true },
                )

            val updateReq =
                AgentKnowledgeRequestDto(
                    name = "Updated Knowledge",
                    sourceType = "URL",
                    sourceUri = "https://new.com",
                    metadata = mapOf("updated" to true),
                    embeddingModel = "text-embedding-3-small",
                    active = true,
                )

            mockMvc
                .perform(
                    putAuth("/api/agents/${agent.id}/knowledge/${entity.id}", updateReq, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("Updated Knowledge"))
                .andExpect(jsonPath("$.sourceUri").value("https://new.com"))
                .andExpect(jsonPath("$.embeddingModel").value("text-embedding-3-small"))
        }

        @Test
        fun `should soft delete knowledge for agent and exclude from queries`() {
            val entity =
                repository.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Temp Knowledge",
                        sourceType = "URL",
                        sourceUri = "https://delete.me",
                    ).apply { active = true },
                )

            // Call DELETE API
            mockMvc
                .perform(
                    deleteAuth("/api/agents/${agent.id}/knowledge/${entity.id}", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isNoContent)

            // Verify soft delete in DB (deleted_at not null)
            assertSoftDeleted(entityManager, AgentKnowledge::class.java, entity.id!!)

            // Verify excluded from GET /api/agents/{agentId}/knowledge
            mockMvc
                .perform(
                    getAuth("/api/agents/${agent.id}/knowledge", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$").isEmpty)
        }

        @Test
        fun `should validate missing name`() {
            val invalidReq =
                AgentKnowledgeRequestDto(
                    name = "   ", // blank
                    sourceType = "URL",
                    sourceUri = "https://example.com",
                )

            mockMvc
                .perform(
                    postAuth("/api/agents/${agent.id}/knowledge", invalidReq, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isBadRequest)
        }
    }

package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.request.UrlKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
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
                        provider = "OpenAI",
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        model = "gpt-4",
                        embeddingModel = "openai/text-embedding-3-small",
                        embeddingsPath = "/embeddings",
                    ),
                )
        }

        @Test
        fun `should create a new agent knowledge`() {
            val req =
                UrlKnowledgeRequestDto(
                    name = "Ophthalmology Dataset",
                    url = "https://example.com/ophthalmology",
                )

            mockMvc
                .perform(
                    postAuth(
                        "/api/agents/${agent.id}/knowledge",
                        req,
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("Ophthalmology Dataset"))
                .andExpect(jsonPath("$.sourceUri").value("https://example.com/ophthalmology"))
                .andExpect(jsonPath("$.sourceType").value("WEB_URL"))
                .andExpect(jsonPath("$.active").value(true))
        }

        @Test
        fun `should get all knowledge for agent`() {
            repository.save(
                AgentKnowledge(
                    agent = agent,
                    name = "Knowledge A",
                    sourceType = KnowledgeSourceType.WEB_URL,
                    sourceUri = "https://a.com",
                ).apply { active = true },
            )
            repository.save(
                AgentKnowledge(
                    agent = agent,
                    name = "Knowledge B",
                    sourceType = KnowledgeSourceType.WEB_URL,
                    sourceUri = "https://b.com",
                ).apply { active = false },
            )

            mockMvc
                .perform(
                    getAuth("/api/agents/${agent.id}/knowledge", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$[0].name").value("Knowledge A"))
                .andExpect(jsonPath("$[1].name").value("Knowledge B"))
        }

        @Test
        fun `should get knowledge by id for agent`() {
            val entity =
                repository.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Knowledge X",
                        sourceType = KnowledgeSourceType.WEB_URL,
                        sourceUri = "https://example.com/x",
                    ).apply { active = true },
                )

            mockMvc
                .perform(
                    getAuth(
                        "/api/agents/${agent.id}/knowledge/${entity.id}",
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(entity.id.toString()))
                .andExpect(jsonPath("$.name").value("Knowledge X"))
        }

        @Test
        fun `should update existing knowledge for agent`() {
            val entity =
                repository.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Old Knowledge",
                        sourceType = KnowledgeSourceType.WEB_URL,
                        sourceUri = "https://old.com",
                    ).apply { active = true },
                )

            val updateReq =
                UrlKnowledgeRequestDto(
                    name = "Updated Knowledge",
                    url = "https://new.com",
                )

            mockMvc
                .perform(
                    putAuth(
                        "/api/agents/${agent.id}/knowledge/${entity.id}",
                        updateReq,
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("Updated Knowledge"))
                .andExpect(jsonPath("$.sourceUri").value("https://new.com"))
                .andExpect(jsonPath("$.sourceType").value("WEB_URL"))
        }

        @Test
        fun `should soft delete knowledge for agent and exclude from queries`() {
            val entity =
                repository.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Temp Knowledge",
                        sourceType = KnowledgeSourceType.WEB_URL,
                        sourceUri = "https://delete.me",
                    ).apply { active = true },
                )

            // Delete
            mockMvc
                .perform(
                    deleteAuth(
                        "/api/agents/${agent.id}/knowledge/${entity.id}",
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isNoContent)

            // Verify DB deleted flag
            assertSoftDeleted(entityManager, AgentKnowledge::class.java, entity.id!!)

            // GET still returns but should show active=false
            mockMvc
                .perform(
                    getAuth(
                        "/api/agents/${agent.id}/knowledge/${entity.id}",
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.active").value(false))
        }

        @Test
        fun `should validate missing name`() {
            val invalidReq =
                UrlKnowledgeRequestDto(
                    name = "   ", // invalid blank
                    url = "https://example.com",
                )

            mockMvc
                .perform(
                    postAuth(
                        "/api/agents/${agent.id}/knowledge",
                        invalidReq,
                        roles = listOf("ROLE_ADMIN"),
                    ),
                ).andExpect(status().isBadRequest)
        }
    }

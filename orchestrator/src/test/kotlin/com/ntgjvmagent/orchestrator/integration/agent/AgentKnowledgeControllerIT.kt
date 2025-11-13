package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
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
        private val entityManager: EntityManager,
    ) : BaseIntegrationTest() {
        @BeforeEach
        fun setup() {
            repository.deleteAll()
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
                    postAuth("/api/knowledge", req, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("Ophthalmology Dataset"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.embeddingModel").value("text-embedding-3-small"))
        }

        @Test
        fun `should get all active knowledge sources`() {
            repository.save(
                AgentKnowledge(
                    name = "Knowledge A",
                    sourceType = "URL",
                    sourceUri = "https://a.com",
                ).apply { active = true },
            )
            repository.save(
                AgentKnowledge(
                    name = "Knowledge B",
                    sourceType = "URL",
                    sourceUri = "https://b.com",
                ).apply { active = false },
            )

            mockMvc
                .perform(
                    getAuth("/api/knowledge", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$[0].name").value("Knowledge A"))
                .andExpect(jsonPath("$[0].active").value(true))
        }

        @Test
        fun `should get knowledge by id`() {
            val entity =
                repository.save(
                    AgentKnowledge(
                        name = "Knowledge X",
                        sourceType = "URL",
                        sourceUri = "https://example.com/x",
                        embeddingModel = "text-embedding-3-large",
                    ).apply { active = true },
                )

            mockMvc
                .perform(
                    getAuth("/api/knowledge/${entity.id}", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(entity.id.toString()))
                .andExpect(jsonPath("$.name").value("Knowledge X"))
                .andExpect(jsonPath("$.embeddingModel").value("text-embedding-3-large"))
        }

        @Test
        fun `should update existing knowledge`() {
            val entity =
                repository.save(
                    AgentKnowledge(
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
                    putAuth("/api/knowledge/${entity.id}", updateReq, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("Updated Knowledge"))
                .andExpect(jsonPath("$.sourceUri").value("https://new.com"))
                .andExpect(jsonPath("$.embeddingModel").value("text-embedding-3-small"))
        }

        @Test
        fun `should soft delete knowledge and exclude from queries`() {
            val entity =
                repository.save(
                    AgentKnowledge(
                        name = "Temp Knowledge",
                        sourceType = "URL",
                        sourceUri = "https://delete.me",
                    ).apply { active = true },
                )

            // Call DELETE API
            mockMvc
                .perform(
                    deleteAuth("/api/knowledge/${entity.id}", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isNoContent)

            // Verify soft delete in DB (deleted_at not null)
            assertSoftDeleted(entityManager, AgentKnowledge::class.java, entity.id!!)

            // Verify excluded from GET /api/knowledge
            mockMvc
                .perform(
                    getAuth("/api/knowledge", roles = listOf("ROLE_ADMIN")),
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
                    postAuth("/api/knowledge", invalidReq, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isBadRequest)
        }
    }

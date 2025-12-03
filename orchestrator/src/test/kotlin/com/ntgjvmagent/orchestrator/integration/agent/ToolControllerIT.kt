package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.request.ToolRequestDto
import com.ntgjvmagent.orchestrator.entity.Tool
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import com.ntgjvmagent.orchestrator.support.SoftDeleteAssertions.assertSoftDeleted
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ToolControllerIT
    @Autowired
    constructor(
        private val repository: ToolRepository,
        private val entityManager: EntityManager,
    ) : BaseIntegrationTest() {
        @BeforeEach
        fun setup() {
            repository.deleteAll()
        }

        @Test
        fun `should create a new agent tool`() {
            val request =
                ToolRequestDto(
                    name = "LangChain",
                    description = "A framework for building LLM applications",
                    active = true,
                )

            mockMvc
                .perform(
                    postAuth("/api/tools", request, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("LangChain"))
                .andExpect(jsonPath("$.active").value(true))
        }

        @Test
        fun `should get all active tools`() {
            repository.save(
                Tool(
                    name = "Tool A",
                    description = "Active Tool",
                ).apply { active = true },
            )
            repository.save(
                Tool(
                    name = "Tool B",
                    description = "Inactive Tool",
                ).apply { active = false },
            )

            mockMvc
                .perform(getAuth("/api/tools", roles = listOf("ROLE_ADMIN")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].name").value("Tool A"))
                .andExpect(jsonPath("$[0].active").value(true))
        }

        @Test
        fun `should get tool by id`() {
            val entity =
                repository.save(
                    Tool(
                        name = "Tool C",
                        description = "Testing retrieval",
                    ).apply { active = true },
                )

            mockMvc
                .perform(
                    getAuth("/api/tools/${entity.id}", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(entity.id.toString()))
                .andExpect(jsonPath("$.name").value("Tool C"))
        }

        @Test
        fun `should update tool`() {
            val entity =
                repository.save(
                    Tool(
                        name = "Old Tool",
                        description = "Before update",
                    ).apply { active = true },
                )

            val updateRequest =
                ToolRequestDto(
                    name = "Updated Tool",
                    description = "After update",
                    active = true,
                )

            mockMvc
                .perform(
                    putAuth("/api/tools/${entity.id}", updateRequest, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("Updated Tool"))
        }

        @Test
        fun `should soft delete tool and exclude from future queries`() {
            val entity =
                repository.save(
                    Tool(
                        name = "Tool D",
                        description = "To be soft deleted",
                    ).apply { active = true },
                )

            // Perform DELETE API call
            mockMvc
                .perform(
                    deleteAuth("/api/tools/${entity.id}", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isNoContent)

            // Verify soft delete applied (deleted_at not null)
            assertSoftDeleted(entityManager, Tool::class.java, entity.id!!)

            // Verify excluded from normal getAll()
            mockMvc
                .perform(getAuth("/api/tools", roles = listOf("ROLE_ADMIN")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isEmpty)
        }
    }

package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.AgentToolRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.service.AgentToolService
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Transactional
class AgentToolServiceIT
    @Autowired
    constructor(
        private val service: AgentToolService,
        private val repo: AgentToolRepository,
    ) : BaseIntegrationTest() {
        private lateinit var tool: AgentTool

        @BeforeEach
        fun setUp() {
            repo.deleteAll()
            tool =
                repo.save(
                    AgentTool(
                        name = "Laser Gun",
                        type = "Weapon",
                        description = "High energy laser weapon",
                        config = mapOf("power" to 9001),
                    ).apply { active = true },
                )
        }

        @Test
        fun `getAllActive should return only active tools`() {
            val result = service.getAllActive()
            assertEquals(1, result.size)
            assertEquals(tool.id, result.first().id)
        }

        @Test
        fun `getById should return tool by id`() {
            val result = service.getById(tool.id!!)
            assertEquals(tool.id, result.id)
            assertEquals(tool.name, result.name)
        }

        @Test
        fun `getById should throw EntityNotFoundException for non-existing id`() {
            val randomId = UUID.randomUUID()
            val exception =
                assertThrows<EntityNotFoundException> {
                    service.getById(randomId)
                }
            assertTrue(exception.message!!.contains("Tool not found"))
        }

        @Test
        fun `create should save new tool`() {
            val request =
                AgentToolRequestDto(
                    name = "Plasma Rifle",
                    type = "Weapon",
                    description = "Plasma energy rifle",
                    config = mapOf("range" to 300),
                    active = true,
                )
            val result = service.create(request)
            assertEquals(request.name, result.name)
            assertTrue(repo.existsById(result.id))
        }

        @Test
        fun `update should modify existing tool`() {
            val updateRequest =
                AgentToolRequestDto(
                    name = "Laser Blaster",
                    type = "Weapon",
                    description = "Upgraded laser weapon",
                    config = mapOf("power" to 12000),
                    active = false,
                )

            val result = service.update(tool.id!!, updateRequest)
            assertEquals("Laser Blaster", result.name)
            assertEquals(false, result.active)
            assertEquals(12000, result.config?.get("power"))
        }

        @Test
        fun `softDelete should mark tool as deleted`() {
            service.softDelete(tool.id!!)
            val deleted = repo.findById(tool.id!!).get()
            assertTrue(deleted.deletedAt != null)
        }

        @Test
        fun `softDelete should throw exception for non-existing tool`() {
            val randomId = UUID.randomUUID()
            val exception =
                assertThrows<EntityNotFoundException> {
                    service.softDelete(randomId)
                }
            assertTrue(exception.message!!.contains("Tool not found"))
        }
    }

package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.internal.ToolDataDto
import com.ntgjvmagent.orchestrator.dto.request.AuthenticationRequestDto
import com.ntgjvmagent.orchestrator.dto.request.ToolRequestDto
import com.ntgjvmagent.orchestrator.entity.Tool
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import com.ntgjvmagent.orchestrator.service.ToolService
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
class ToolServiceIT
    @Autowired
    constructor(
        private val service: ToolService,
        private val repo: ToolRepository,
    ) : BaseIntegrationTest() {
        private lateinit var tool: Tool

        @BeforeEach
        fun setUp() {
            repo.deleteAll()
            repo.flush()
            tool =
                repo.save(
                    Tool(
                        name = "Laser Gun",
                        type = "Weapon",
                        description = "High energy laser weapon",
                        connectionConfig = mapOf("power" to 9001),
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
                ToolRequestDto(
                    baseUrl = "https://docs.mcp.cloudflare.com",
                    transportType = "SSE",
                    endpoint = "/sse",
                    authorization = AuthenticationRequestDto(),
                )
            service.create(request)
        }

        @Test
        fun `update should modify existing tool`() {
            val updateRequest =
                ToolDataDto(
                    name = "Laser Blaster",
                    type = "Weapon",
                    description = "Upgraded laser weapon",
                    definition = mapOf("power" to 12000),
                    active = false,
                )

            val result = service.update(tool.id!!, updateRequest)
            assertEquals("Laser Blaster", result.name)
            assertEquals(false, result.active)
            assertEquals(12000, result.definition?.get("power"))
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

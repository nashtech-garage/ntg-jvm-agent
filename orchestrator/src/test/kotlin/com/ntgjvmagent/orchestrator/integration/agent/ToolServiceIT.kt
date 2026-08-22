package com.ntgjvmagent.orchestrator.integration.agent

import com.ninjasquad.springmockk.MockkBean
import com.ntgjvmagent.orchestrator.dto.internal.ToolDataDto
import com.ntgjvmagent.orchestrator.dto.request.AuthenticationRequestDto
import com.ntgjvmagent.orchestrator.dto.request.ToolRequestDto
import com.ntgjvmagent.orchestrator.entity.Tool
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import com.ntgjvmagent.orchestrator.service.LeasedToolCallbacks
import com.ntgjvmagent.orchestrator.service.McpConnection
import com.ntgjvmagent.orchestrator.service.McpToolDiscovery
import com.ntgjvmagent.orchestrator.service.ToolService
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.utils.McpClientTransportType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
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
        @MockkBean
        lateinit var mcpToolDiscovery: McpToolDiscovery

        private lateinit var tool: Tool

        @BeforeEach
        fun setUp() {
            val definition = mockk<ToolDefinition>()
            every { definition.name() } returns "getCurrentDatetime"
            every { definition.description() } returns "Get the current date and time"
            every { definition.inputSchema() } returns "{\"type\":\"object\",\"properties\":{}}"
            val callback = mockk<ToolCallback>()
            every { callback.toolDefinition } returns definition
            every { mcpToolDiscovery.discover(any(), any(), any(), any()) } answers {
                LeasedToolCallbacks(listOf(callback)) {}
            }
            every { mcpToolDiscovery.verify(any(), any(), any(), any()) } returns listOf(callback)
            every { mcpToolDiscovery.retainActiveConnections(any()) } just Runs

            repo.deleteAll()
            repo.flush()
            tool =
                repo.save(
                    Tool(
                        name = "Laser Gun",
                        type = Constant.MCP_TOOL_TYPE,
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
                    baseUrl = "http://localhost:19003",
                    transportType = "STREAMABLE",
                    endpoint = "/mcp",
                    authorization = AuthenticationRequestDto(),
                )
            service.create(request)
            assertTrue(repo.findAll().any { it.name == "getCurrentDatetime" })
        }

        @Test
        fun `loading external callbacks retains only active database connections`() {
            val external =
                repo.save(
                    Tool(
                        name = "External datetime",
                        type = Constant.MCP_TOOL_TYPE,
                        baseUrl = "http://localhost:19003",
                        connectionConfig =
                            mapOf(
                                "endpoint" to "/mcp",
                                "transportType" to "STREAMABLE",
                                "authorization" to mapOf("type" to "NONE"),
                            ),
                    ).apply { active = true },
                )

            service.loadExternalToolCallbackFromDb().use { assertEquals(1, it.callbacks.size) }
            verify {
                mcpToolDiscovery.retainActiveConnections(
                    setOf(
                        McpConnection(
                            "http://localhost:19003",
                            "/mcp",
                            AuthenticationRequestDto(),
                            McpClientTransportType.STREAMABLE,
                        ),
                    ),
                )
            }

            external.active = false
            repo.saveAndFlush(external)
            service.loadExternalToolCallbackFromDb().use { assertTrue(it.callbacks.isEmpty()) }
            verify { mcpToolDiscovery.retainActiveConnections(emptySet()) }
        }

        @Test
        fun `update should modify existing tool`() {
            val updateRequest =
                ToolDataDto(
                    name = "Laser Blaster",
                    type = Constant.MCP_TOOL_TYPE,
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

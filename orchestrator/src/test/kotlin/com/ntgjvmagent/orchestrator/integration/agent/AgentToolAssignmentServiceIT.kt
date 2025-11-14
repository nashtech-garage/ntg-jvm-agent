package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.service.AgentToolAssignmentService
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Transactional
class AgentToolAssignmentServiceIT
    @Autowired
    constructor(
        private val agentRepository: AgentRepository,
        private val toolRepository: AgentToolRepository,
        private val agentToolAssignmentService: AgentToolAssignmentService,
    ) : BaseIntegrationTest() {
        private lateinit var agent: Agent
        private lateinit var activeTool: AgentTool
        private lateinit var inactiveTool: AgentTool

        @BeforeAll
        fun initData() {
            agent = agentRepository.save(Agent(name = "Agent Smith ${UUID.randomUUID()}", model = "T-800"))
            activeTool = toolRepository.save(AgentTool(name = "Laser Gun ${UUID.randomUUID()}").apply { active = true })
            inactiveTool =
                toolRepository.save(AgentTool(name = "Inactive Gadget ${UUID.randomUUID()}").apply { active = false })
        }

        @Test
        fun `assignTool should assign active tool to agent`() {
            agentToolAssignmentService.assignTool(agent.id!!, activeTool.id!!)

            val tools = agentToolAssignmentService.getTools(agent.id!!)
            assertEquals(1, tools.size)
            assertEquals(activeTool.id, tools.first().id)
        }

        @Test
        fun `assignTool should fail when tool is inactive`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    agentToolAssignmentService.assignTool(agent.id!!, inactiveTool.id!!)
                }
            assertEquals("Cannot assign inactive tool to agent", exception.message)
        }

        @Test
        fun `assignTool should fail when tool already assigned`() {
            agentToolAssignmentService.assignTool(agent.id!!, activeTool.id!!)

            val exception =
                assertThrows<IllegalArgumentException> {
                    agentToolAssignmentService.assignTool(agent.id!!, activeTool.id!!)
                }
            assertEquals("Tool already assigned to this agent", exception.message)
        }

        @Test
        fun `assignTool should fail when agent or tool not found`() {
            val nonExistentAgentId = UUID.randomUUID()
            val nonExistentToolId = UUID.randomUUID()

            val exception1 =
                assertThrows<EntityNotFoundException> {
                    agentToolAssignmentService.assignTool(nonExistentAgentId, activeTool.id!!)
                }
            assertTrue(exception1.message!!.contains("Agent not found"))

            val exception2 =
                assertThrows<EntityNotFoundException> {
                    agentToolAssignmentService.assignTool(agent.id!!, nonExistentToolId)
                }
            assertTrue(exception2.message!!.contains("Tool not found"))
        }

        @Test
        fun `unassignTool should remove mapping`() {
            agentToolAssignmentService.assignTool(agent.id!!, activeTool.id!!)
            agentToolAssignmentService.unassignTool(agent.id!!, activeTool.id!!)

            val tools = agentToolAssignmentService.getTools(agent.id!!)
            assertTrue(tools.isEmpty())
        }

        @Test
        fun `getTools should return empty list if no tools assigned`() {
            val tools = agentToolAssignmentService.getTools(agent.id!!)
            assertTrue(tools.isEmpty())
        }
    }

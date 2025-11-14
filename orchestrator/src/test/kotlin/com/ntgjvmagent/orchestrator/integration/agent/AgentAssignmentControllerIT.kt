package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.entity.agent.mapping.AgentKnowledgeMapping
import com.ntgjvmagent.orchestrator.entity.agent.mapping.AgentToolMapping
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeMappingRepository
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.AgentToolMappingRepository
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class AgentAssignmentControllerIT
    @Autowired
    constructor(
        private val agentRepository: AgentRepository,
        private val toolRepository: AgentToolRepository,
        private val mappingRepository: AgentToolMappingRepository,
        private val knowledgeRepository: AgentKnowledgeRepository,
        private val agentKnowledgeMappingRepository: AgentKnowledgeMappingRepository,
    ) : BaseIntegrationTest() {
        private lateinit var agent: Agent
        private lateinit var activeTool: AgentTool
        private lateinit var knowledge: AgentKnowledge

        @BeforeAll
        fun setUp() {
            agent =
                agentRepository.save(
                    Agent(name = "Agent Smith", model = "T-800"),
                )
            activeTool =
                toolRepository.save(
                    AgentTool(name = "Laser Gun").apply { active = true },
                )
            knowledge =
                knowledgeRepository.save(
                    AgentKnowledge(name = "K1-${UUID.randomUUID()}"),
                )
        }

        @Test
        fun `POST assign tool should assign tool to agent`() {
            mockMvc
                .perform(
                    postAuth("/api/agents/${agent.id}/tools/${activeTool.id}", {}, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)

            // Verify mapping exists
            val assignedTools = mappingRepository.findByAgentId(agent.id!!)
            assert(assignedTools.any { it.tool.id == activeTool.id })
        }

        @Test
        fun `DELETE unassign tool should remove tool from agent`() {
            // Assign first
            mappingRepository.save(AgentToolMapping.of(agent, activeTool))

            mockMvc
                .perform(
                    deleteAuth("/api/agents/${agent.id}/tools/${activeTool.id}", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)

            val assignedTools = mappingRepository.findByAgentId(agent.id!!)
            assert(assignedTools.none { it.tool.id == activeTool.id })
        }

        @Test
        fun `GET list tools should return assigned tools`() {
            mappingRepository.save(AgentToolMapping.of(agent, activeTool))

            mockMvc
                .perform(
                    getAuth("/api/agents/${agent.id}/tools", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$[0].id").value(activeTool.id.toString()))
                .andExpect(jsonPath("$[0].name").value(activeTool.name))
        }

        @Test
        fun `POST assign knowledge should assign knowledge to agent`() {
            mockMvc
                .perform(
                    postAuth("/api/agents/${agent.id}/knowledge/${knowledge.id}", {}, roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)

            val assignedKnowledge = agentKnowledgeMappingRepository.findByAgentId(agent.id!!)
            assert(assignedKnowledge.any { it.knowledge.id == knowledge.id })
        }

        @Test
        fun `DELETE unassign knowledge should remove knowledge from agent`() {
            agentKnowledgeMappingRepository.save(AgentKnowledgeMapping.of(agent, knowledge))

            mockMvc
                .perform(
                    deleteAuth("/api/agents/${agent.id}/knowledge/${knowledge.id}", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)

            val assignedKnowledge = agentKnowledgeMappingRepository.findByAgentId(agent.id!!)
            assert(assignedKnowledge.none { it.knowledge.id == knowledge.id })
        }

        @Test
        fun `GET list knowledge should return assigned knowledge`() {
            agentKnowledgeMappingRepository.save(AgentKnowledgeMapping.of(agent, knowledge))

            mockMvc
                .perform(
                    getAuth("/api/agents/${agent.id}/knowledge", roles = listOf("ROLE_ADMIN")),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$[0].id").value(knowledge.id.toString()))
                .andExpect(jsonPath("$[0].name").value(knowledge.name))
        }
    }

package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.entity.agent.mapping.AgentToolMapping
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.AgentToolMappingRepository
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AgentToolAssignmentService(
    private val agentRepository: AgentRepository,
    private val toolRepository: AgentToolRepository,
    private val mappingRepository: AgentToolMappingRepository,
) {
    @Transactional
    fun assignTool(
        agentId: UUID,
        toolId: UUID,
    ) {
        val agent = agentRepository.findByIdOrNull(agentId)
        val tool = toolRepository.findByIdOrNull(toolId)

        if (agent == null || tool == null) {
            throw EntityNotFoundException(
                buildString {
                    if (agent == null) append("Agent not found: $agentId. ")
                    if (tool == null) append("Tool not found: $toolId.")
                }.trim(),
            )
        }

        require(tool.active) { "Cannot assign inactive tool to agent" }
        require(!mappingRepository.existsByAgentIdAndToolId(agentId, toolId)) {
            "Tool already assigned to this agent"
        }

        mappingRepository.save(AgentToolMapping.of(agent, tool))
    }

    @Transactional
    fun unassignTool(
        agentId: UUID,
        toolId: UUID,
    ) {
        mappingRepository.deleteByAgentIdAndToolId(agentId, toolId)
    }

    @Transactional(readOnly = true)
    fun getTools(agentId: UUID): List<AgentTool> = mappingRepository.findByAgentId(agentId).map { it.tool }
}

package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.response.AgentToolResponseDto
import com.ntgjvmagent.orchestrator.dto.response.ToolWithAssignmentResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.mapper.AgentToolMapper
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AgentToolService(
    private val agentRepository: AgentRepository,
    private val toolRepository: ToolRepository,
    private val agentToolRepository: AgentToolRepository,
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
        require(!agentToolRepository.existsByAgentIdAndToolId(agentId, toolId)) {
            "Tool already assigned to this agent"
        }

        agentToolRepository.save(AgentTool.of(agent, tool))
    }

    @Transactional
    fun unassignTool(
        agentId: UUID,
        toolId: UUID,
    ) {
        agentToolRepository.deleteByAgentIdAndToolId(agentId, toolId)
    }

    @Transactional(readOnly = true)
    fun getTools(agentId: UUID): List<AgentToolResponseDto> =
        agentToolRepository
            .findByAgentId(agentId)
            .map(AgentToolMapper::toResponse)

    @Transactional(readOnly = true)
    fun getToolsWithAssignmentStatus(agentId: UUID): List<ToolWithAssignmentResponseDto> =
        agentToolRepository
            .findToolsWithAssignment(agentId)

    @Transactional
    fun updateStatus(
        agentId: UUID,
        toolId: UUID,
        status: Boolean,
    ) {
        val agentTool =
            agentToolRepository.findByAgentIdAndToolId(agentId, toolId)
                ?: throw EntityNotFoundException("Agent tool not found")

        agentTool.active = status
        agentToolRepository.save(agentTool)
    }
}

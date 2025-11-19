package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.AgentToolResponseDto
import com.ntgjvmagent.orchestrator.entity.Tool
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool

object AgentToolMapper {
    fun toResponse(entity: AgentTool): AgentToolResponseDto =
        AgentToolResponseDto(
            id = entity.id!!,
            agentId = entity.agent.id!!,
            toolId = entity.tool.id!!,
            config = entity.config,
            createdAt = entity.createdAt!!,
            updatedAt = entity.updatedAt,
        )

    fun toEntity(
        agent: Agent,
        tool: Tool,
        config: Map<String, Any>? = null,
    ): AgentTool = AgentTool(agent = agent, tool = tool, config = config)
}

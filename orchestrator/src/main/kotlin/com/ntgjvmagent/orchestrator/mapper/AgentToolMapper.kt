package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.AgentToolRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentToolResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool

object AgentToolMapper {
    fun toEntity(req: AgentToolRequestDto) =
        AgentTool(
            name = req.name,
            type = req.type,
            description = req.description,
            config = req.config,
        ).apply { active = req.active }

    fun toResponse(entity: AgentTool) =
        AgentToolResponseDto(
            id = entity.id!!,
            name = entity.name,
            type = entity.type,
            description = entity.description,
            config = entity.config,
            active = entity.active,
        )
}

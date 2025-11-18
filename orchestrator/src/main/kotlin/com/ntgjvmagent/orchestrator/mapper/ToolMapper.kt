package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.ToolRequestDto
import com.ntgjvmagent.orchestrator.dto.ToolResponseDto
import com.ntgjvmagent.orchestrator.entity.Tool

object ToolMapper {
    fun toEntity(req: ToolRequestDto) =
        Tool(
            name = req.name,
            type = req.type,
            description = req.description,
            config = req.config,
        ).apply { active = req.active }

    fun toResponse(entity: Tool) =
        ToolResponseDto(
            id = entity.id!!,
            name = entity.name,
            type = entity.type,
            description = entity.description,
            config = entity.config,
            active = entity.active,
        )
}

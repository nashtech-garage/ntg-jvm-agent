package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.internal.ToolDataDto
import com.ntgjvmagent.orchestrator.dto.response.ToolResponseDto
import com.ntgjvmagent.orchestrator.entity.Tool

object ToolMapper {
    fun toEntity(req: ToolDataDto) =
        Tool(
            name = req.name,
            type = req.type,
            baseUrl = req.baseUrl,
            description = req.description,
            definition = req.definition,
            connectionConfig = req.config,
        ).apply { active = req.active }

    fun toResponse(entity: Tool) =
        ToolResponseDto(
            id = entity.id!!,
            name = entity.name,
            type = entity.type,
            description = entity.description,
            definition = entity.definition,
            active = entity.active,
        )
}

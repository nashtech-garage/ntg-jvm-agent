package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.response.AgentToolResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.utils.toRelativeString

object AgentToolMapper {
    fun toResponse(entity: AgentTool): AgentToolResponseDto =
        AgentToolResponseDto(
            toolId = entity.tool.id!!,
            toolName = entity.tool.name,
            toolType = entity.tool.type,
            availableTo = entity.agent.name,
            lastModifiedBy = entity.updatedBy?.name ?: "Unknown",
            lastModifiedWhen = entity.updatedAt?.toRelativeString(),
            enabled = true,
        )
}

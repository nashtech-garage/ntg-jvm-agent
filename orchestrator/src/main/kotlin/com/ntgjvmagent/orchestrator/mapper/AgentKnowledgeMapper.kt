package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge

object AgentKnowledgeMapper {
    fun toEntity(req: AgentKnowledgeRequestDto) =
        AgentKnowledge(
            name = req.name,
            sourceType = req.sourceType,
            sourceUri = req.sourceUri,
            metadata = req.metadata,
            embeddingModel = req.embeddingModel,
        ).apply { active = req.active }

    fun toResponse(entity: AgentKnowledge) =
        AgentKnowledgeResponseDto(
            id = entity.id!!,
            name = entity.name,
            sourceType = entity.sourceType,
            sourceUri = entity.sourceUri,
            metadata = entity.metadata,
            embeddingModel = entity.embeddingModel,
            active = entity.active,
        )
}

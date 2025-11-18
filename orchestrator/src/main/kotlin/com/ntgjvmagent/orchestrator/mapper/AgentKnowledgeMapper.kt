package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge

object AgentKnowledgeMapper {
    fun toResponse(entity: AgentKnowledge): AgentKnowledgeResponseDto =
        AgentKnowledgeResponseDto(
            id = entity.id!!,
            agentId = entity.agent.id!!,
            name = entity.name,
            sourceType = entity.sourceType,
            sourceUri = entity.sourceUri,
            metadata = entity.metadata,
            embeddingModel = entity.embeddingModel,
            active = entity.active,
            createdAt = entity.createdAt!!,
            updatedAt = entity.updatedAt,
        )
}

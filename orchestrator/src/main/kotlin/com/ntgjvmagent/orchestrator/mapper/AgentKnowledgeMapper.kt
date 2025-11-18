package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge

object AgentKnowledgeMapper {
    fun toEntity(
        agent: Agent,
        request: AgentKnowledgeRequestDto,
    ) = AgentKnowledge(
        agent = agent,
        name = request.name,
        sourceType = request.sourceType,
        sourceUri = request.sourceUri,
        metadata = request.metadata,
        embeddingModel = request.embeddingModel,
    )

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

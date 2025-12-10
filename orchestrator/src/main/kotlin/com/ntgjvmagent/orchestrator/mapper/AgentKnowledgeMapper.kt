package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.request.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeListResponseDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.model.FileKnowledgeInternalRequest
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.utils.toRelativeString

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
    )

    fun toEntity(
        agent: Agent,
        request: FileKnowledgeInternalRequest,
    ) = AgentKnowledge(
        agent = agent,
        name = request.name,
        sourceType = KnowledgeSourceType.FILE,
        sourceUri = null,
        metadata = request.metadata,
    )

    fun toResponse(entity: AgentKnowledge): AgentKnowledgeResponseDto =
        AgentKnowledgeResponseDto(
            id = entity.id!!,
            agentId = entity.agent.id!!,
            name = entity.name,
            sourceType = entity.sourceType,
            sourceUri = entity.sourceUri,
            metadata = entity.metadata,
            active = entity.active,
            createdAt = entity.createdAt!!,
            updatedAt = entity.updatedAt,
        )

    fun toListResponse(entity: AgentKnowledge): AgentKnowledgeListResponseDto =
        AgentKnowledgeListResponseDto(
            id = entity.id!!,
            name = entity.name,
            type = entity.sourceType,
            availableTo = entity.agent.name,
            usage = "",
            lastModifiedBy = entity.updatedBy?.name ?: "Unknown",
            lastModifiedWhen = entity.updatedAt?.toRelativeString(),
            status = entity.status,
        )
}

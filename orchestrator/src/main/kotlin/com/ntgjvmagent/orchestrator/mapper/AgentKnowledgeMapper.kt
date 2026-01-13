package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.request.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeListResponseDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.storage.facade.StoredFileMetadata
import com.ntgjvmagent.orchestrator.utils.toRelativeString

object AgentKnowledgeMapper {
    // -------------------------------
    // Non-file
    // -------------------------------
    fun toEntity(
        agent: Agent,
        name: String,
        request: AgentKnowledgeRequestDto,
    ) = AgentKnowledge(
        agent = agent,
        name = name,
        sourceType = request.sourceType,
        sourceUri = request.sourceUri,
        metadata = request.metadata,
    )

    // -------------------------------
    // File-based (internal only)
    // -------------------------------
    fun toFileEntity(
        agent: Agent,
        name: String,
        originalFileName: String,
        storageKey: String,
        stored: StoredFileMetadata,
        metadata: Map<String, Any?>,
    ) = AgentKnowledge(
        agent = agent,
        name = name,
        sourceType = KnowledgeSourceType.FILE,
        sourceUri = null,
        originalFileName = originalFileName,
        storageKey = storageKey,
        checksumSha256 = stored.checksumSha256,
        fileSizeBytes = stored.fileSizeBytes,
        metadata = metadata,
        status = KnowledgeStatus.PENDING,
    )

    // -------------------------------
    // Responses
    // -------------------------------
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
            sourceUri = entity.sourceUri,
            availableTo = entity.agent.name,
            usage = "",
            lastModifiedBy = entity.updatedBy?.name ?: "Unknown",
            lastModifiedWhen = entity.updatedAt?.toRelativeString(),
            status = entity.status,
        )
}

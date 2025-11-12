package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.mapper.AgentKnowledgeMapper
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AgentKnowledgeService(
    private val repo: AgentKnowledgeRepository,
) {
    @Transactional(readOnly = true)
    fun getAllActive(): List<AgentKnowledgeResponseDto> =
        repo.findAllByActiveTrue().map(AgentKnowledgeMapper::toResponse)

    @Transactional(readOnly = true)
    fun getById(id: UUID): AgentKnowledgeResponseDto =
        repo
            .findById(id)
            .map(AgentKnowledgeMapper::toResponse)
            .orElseThrow { EntityNotFoundException("Knowledge not found: $id") }

    @Transactional
    fun create(request: AgentKnowledgeRequestDto): AgentKnowledgeResponseDto {
        val entity = AgentKnowledgeMapper.toEntity(request)
        return AgentKnowledgeMapper.toResponse(repo.save(entity))
    }

    @Transactional
    fun update(
        id: UUID,
        request: AgentKnowledgeRequestDto,
    ): AgentKnowledgeResponseDto {
        val existing =
            repo
                .findById(id)
                .orElseThrow { EntityNotFoundException("Knowledge not found: $id") }

        existing.apply {
            name = request.name
            sourceType = request.sourceType
            sourceUri = request.sourceUri
            metadata = request.metadata
            embeddingModel = request.embeddingModel
            active = request.active
        }
        return AgentKnowledgeMapper.toResponse(repo.save(existing))
    }

    @Transactional
    fun softDelete(id: UUID) {
        val knowledge =
            repo
                .findById(id)
                .orElseThrow { EntityNotFoundException("Knowledge not found: $id") }
        knowledge.markDeleted()
        repo.save(knowledge)
    }
}

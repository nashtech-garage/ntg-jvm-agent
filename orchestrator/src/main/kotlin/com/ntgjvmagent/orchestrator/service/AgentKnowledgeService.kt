package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.request.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeListResponseDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.mapper.AgentKnowledgeMapper
import com.ntgjvmagent.orchestrator.model.FileKnowledgeInternalRequest
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AgentKnowledgeService(
    private val repo: AgentKnowledgeRepository,
    private val agentRepo: AgentRepository,
) {
    @Transactional(readOnly = true)
    fun getByAgent(
        agentId: UUID,
        name: String?,
    ): List<AgentKnowledgeListResponseDto> =
        repo
            .let {
                if (name.isNullOrBlank()) {
                    it.findAllByAgentId(agentId)
                } else {
                    it.searchByAgentAndName(agentId, name.trim())
                }
            }.map(AgentKnowledgeMapper::toListResponse)

    @Transactional(readOnly = true)
    fun getOneForAgent(
        agentId: UUID,
        knowledgeId: UUID,
    ): AgentKnowledgeResponseDto {
        val entity =
            repo.findByIdAndAgentId(knowledgeId, agentId)
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")

        return AgentKnowledgeMapper.toResponse(entity)
    }

    @Transactional
    fun create(
        agentId: UUID,
        request: AgentKnowledgeRequestDto,
    ): AgentKnowledgeResponseDto {
        val agent =
            agentRepo.findByIdOrNull(agentId)
                ?: throw EntityNotFoundException("Agent not found: $agentId")

        val entity = AgentKnowledgeMapper.toEntity(agent, request)

        return AgentKnowledgeMapper.toResponse(repo.save(entity))
    }

    @Transactional
    fun create(
        agentId: UUID,
        request: FileKnowledgeInternalRequest,
    ): AgentKnowledgeResponseDto {
        val agent =
            agentRepo.findByIdOrNull(agentId)
                ?: throw EntityNotFoundException("Agent not found: $agentId")

        val entity = AgentKnowledgeMapper.toEntity(agent, request)

        return AgentKnowledgeMapper.toResponse(repo.save(entity))
    }

    @Transactional
    fun update(
        agentId: UUID,
        knowledgeId: UUID,
        request: AgentKnowledgeRequestDto,
    ): AgentKnowledgeResponseDto {
        val existing =
            repo.findByIdOrNull(knowledgeId)
                ?: throw EntityNotFoundException("Knowledge not found: $knowledgeId")

        // Prevent agent transfer
        require(existing.agent.id == agentId) {
            "Knowledge $knowledgeId does not belong to agent $agentId"
        }

        existing.apply {
            name = request.name
            sourceType = request.sourceType
            sourceUri = request.sourceUri
            metadata = request.metadata
        }

        return AgentKnowledgeMapper.toResponse(repo.save(existing))
    }

    @Transactional
    fun softDelete(
        agentId: UUID,
        knowledgeId: UUID,
    ) {
        val existing =
            repo.findByIdOrNull(knowledgeId)
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")

        require(existing.agent.id == agentId) {
            "Knowledge $knowledgeId does not belong to agent $agentId"
        }

        existing.markDeleted()
        repo.save(existing)
    }
}

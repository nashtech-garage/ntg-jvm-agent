package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.mapper.AgentKnowledgeMapper
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
    fun getByAgent(agentId: UUID): List<AgentKnowledgeResponseDto> =
        repo.findAllByAgentIdAndActiveTrue(agentId).map(AgentKnowledgeMapper::toResponse)

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
            agentRepo
                .findByIdOrNull(agentId)
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
            repo
                .findByIdOrNull(knowledgeId)
                ?: throw EntityNotFoundException("Knowledge not found: $knowledgeId")

        // Agent cannot be changed → validate agentId
        require(existing.agent.id == agentId) { "Agent cannot be changed for this knowledge item." }

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
    fun softDelete(
        agentId: UUID,
        knowledgeId: UUID,
    ) {
        val knowledge =
            repo
                .findByIdOrNull(knowledgeId)
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")

        require(knowledge.agent.id == agentId) { "Knowledge $knowledgeId does not belong to agent $agentId" }

        knowledge.markDeleted()
        repo.save(knowledge)
    }
}

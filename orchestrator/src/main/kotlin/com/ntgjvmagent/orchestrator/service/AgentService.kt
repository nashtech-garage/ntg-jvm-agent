package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.AgentRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentResponseDto
import com.ntgjvmagent.orchestrator.mapper.AgentMapper
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class AgentService(
    private val repo: AgentRepository,
) {
    @Transactional(readOnly = true)
    fun getAllActive(): List<AgentResponseDto> = repo.findAllActive().map(AgentMapper::toResponse)

    @Transactional(readOnly = true)
    fun getById(id: UUID): AgentResponseDto =
        repo
            .findByIdNotDeleted(id)
            .map(AgentMapper::toResponse)
            .orElseThrow { EntityNotFoundException("Agent not found: $id") }

    @Transactional
    fun create(request: AgentRequestDto): AgentResponseDto {
        val entity = AgentMapper.toEntity(request)
        return AgentMapper.toResponse(repo.save(entity))
    }

    @Transactional
    fun update(
        id: UUID,
        request: AgentRequestDto,
    ): AgentResponseDto {
        val existing =
            repo
                .findByIdNotDeleted(id)
                .orElseThrow { EntityNotFoundException("Agent not found: $id") }

        existing.apply {
            name = request.name
            model = request.model
            description = request.description
            temperature = request.temperature.toBigDecimal()
            maxTokens = request.maxTokens
            topP = request.topP.toBigDecimal()
            frequencyPenalty = request.frequencyPenalty.toBigDecimal()
            presencePenalty = request.presencePenalty.toBigDecimal()
            active = request.active
            provider = request.provider
            settings = request.settings
        }
        return AgentMapper.toResponse(repo.save(existing))
    }

    @Transactional
    fun softDelete(id: UUID) {
        val agent =
            repo
                .findByIdNotDeleted(id)
                .orElseThrow { EntityNotFoundException("Agent not found: $id") }
        agent.deletedAt = ZonedDateTime.now()
        repo.save(agent)
    }
}

package com.ntgjvmagent.orchestrator.agent.service

import com.ntgjvmagent.orchestrator.agent.domain.AgentRepository
import com.ntgjvmagent.orchestrator.agent.web.dto.AgentRequest
import com.ntgjvmagent.orchestrator.agent.web.dto.AgentResponse
import com.ntgjvmagent.orchestrator.agent.web.mapper.AgentMapper
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
    fun getAllActive(): List<AgentResponse> = repo.findAllActive().map(AgentMapper::toResponse)

    @Transactional(readOnly = true)
    fun getById(id: UUID): AgentResponse =
        repo
            .findByIdNotDeleted(id)
            .map(AgentMapper::toResponse)
            .orElseThrow { EntityNotFoundException("Agent not found: $id") }

    @Transactional
    fun create(request: AgentRequest): AgentResponse {
        val entity = AgentMapper.toEntity(request)
        return AgentMapper.toResponse(repo.save(entity))
    }

    @Transactional
    fun update(
        id: UUID,
        request: AgentRequest,
    ): AgentResponse {
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

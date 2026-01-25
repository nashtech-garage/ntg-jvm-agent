package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.request.AgentRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.mapper.AgentMapper
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AgentService(
    private val repo: AgentRepository,
) {
    @Transactional(readOnly = true)
    fun getAll(name: String?) =
        repo
            .let {
                if (name.isNullOrBlank()) {
                    it.findAll()
                } else {
                    it.findByNameContainingIgnoreCase(name.trim())
                }
            }.map(AgentMapper::toListResponse)

    @Transactional(readOnly = true)
    fun getAllActive(): List<AgentResponseDto> = repo.findAllByActiveTrue().map(AgentMapper::toResponse)

    @Transactional(readOnly = true)
    fun getById(id: UUID): AgentResponseDto {
        val entity =
            repo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Agent not found: $id")
        return AgentMapper.toResponse(entity)
    }

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
            repo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Agent not found: $id")

        existing.apply {
            name = request.name
            model = request.model
            description = request.description
            avatar = request.avatar
            temperature = request.temperature.toBigDecimal()
            maxTokens = request.maxTokens
            topP = request.topP.toBigDecimal()
            frequencyPenalty = request.frequencyPenalty.toBigDecimal()
            presencePenalty = request.presencePenalty.toBigDecimal()
            active = request.active
            provider = request.provider
            settings = request.settings
            apiKey = request.apiKey
            baseUrl = request.baseUrl
            chatCompletionsPath = request.chatCompletionsPath
        }
        return AgentMapper.toResponse(repo.save(existing))
    }

    @Transactional
    fun softDelete(id: UUID) {
        val agent =
            repo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Agent not found: $id")
        agent.deletedAt = Instant.now()
        repo.save(agent)
    }
}

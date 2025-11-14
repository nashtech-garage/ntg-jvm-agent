package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.AgentToolRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentToolResponseDto
import com.ntgjvmagent.orchestrator.mapper.AgentToolMapper
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AgentToolService(
    private val repo: AgentToolRepository,
) {
    @Transactional(readOnly = true)
    fun getAllActive(): List<AgentToolResponseDto> = repo.findAllByActiveTrue().map(AgentToolMapper::toResponse)

    @Transactional(readOnly = true)
    fun getById(id: UUID): AgentToolResponseDto =
        repo
            .findById(id)
            .map(AgentToolMapper::toResponse)
            .orElseThrow { EntityNotFoundException("Tool not found: $id") }

    @Transactional
    fun create(request: AgentToolRequestDto): AgentToolResponseDto {
        val entity = AgentToolMapper.toEntity(request)
        return AgentToolMapper.toResponse(repo.save(entity))
    }

    @Transactional
    fun update(
        id: UUID,
        request: AgentToolRequestDto,
    ): AgentToolResponseDto {
        val existing =
            repo
                .findById(id)
                .orElseThrow { EntityNotFoundException("Tool not found: $id") }

        existing.apply {
            name = request.name
            type = request.type
            description = request.description
            config = request.config
            active = request.active
        }
        return AgentToolMapper.toResponse(repo.save(existing))
    }

    @Transactional
    fun softDelete(id: UUID) {
        val tool =
            repo
                .findById(id)
                .orElseThrow { EntityNotFoundException("Tool not found: $id") }
        tool.markDeleted()
        repo.save(tool)
    }
}

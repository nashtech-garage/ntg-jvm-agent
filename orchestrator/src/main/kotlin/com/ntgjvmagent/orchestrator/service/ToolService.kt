package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.request.ToolRequestDto
import com.ntgjvmagent.orchestrator.dto.response.ToolResponseDto
import com.ntgjvmagent.orchestrator.mapper.ToolMapper
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ToolService(
    private val repo: ToolRepository,
) {
    @Transactional(readOnly = true)
    fun getAllActive(): List<ToolResponseDto> = repo.findAllByActiveTrue().map(ToolMapper::toResponse)

    @Transactional(readOnly = true)
    fun getById(id: UUID): ToolResponseDto {
        val entity =
            repo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Tool not found: $id")
        return ToolMapper.toResponse(entity)
    }

    @Transactional
    fun create(request: ToolRequestDto): ToolResponseDto {
        val entity = ToolMapper.toEntity(request)
        return ToolMapper.toResponse(repo.save(entity))
    }

    @Transactional
    fun update(
        id: UUID,
        request: ToolRequestDto,
    ): ToolResponseDto {
        val existing =
            repo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Tool not found: $id")

        existing.apply {
            name = request.name
            type = request.type
            description = request.description
            config = request.config
            active = request.active
        }
        return ToolMapper.toResponse(repo.save(existing))
    }

    @Transactional
    fun softDelete(id: UUID) {
        val tool =
            repo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Tool not found: $id")
        tool.markDeleted()
        repo.save(tool)
    }
}

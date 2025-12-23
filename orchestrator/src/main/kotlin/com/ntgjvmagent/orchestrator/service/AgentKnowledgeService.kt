package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.request.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeListResponseDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.ingestion.orchestrator.IngestionLifecycleService
import com.ntgjvmagent.orchestrator.mapper.AgentKnowledgeMapper
import com.ntgjvmagent.orchestrator.model.FileKnowledgeInternalRequest
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Service
class AgentKnowledgeService(
    private val repo: AgentKnowledgeRepository,
    private val agentRepo: AgentRepository,
    private val ingestionLifecycleService: IngestionLifecycleService,
) {
    @Transactional(readOnly = true)
    fun getByAgent(agentId: UUID): List<AgentKnowledgeListResponseDto> =
        repo
            .findAllByAgentId(agentId)
            .map(AgentKnowledgeMapper::toListResponse)

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

    // -------------------------------
    // CREATE (Generic / Non-file types)
    // -------------------------------
    @Transactional
    fun create(
        agentId: UUID,
        request: AgentKnowledgeRequestDto,
    ): AgentKnowledgeResponseDto {
        val agent =
            agentRepo.findByIdOrNull(agentId)
                ?: throw EntityNotFoundException("Agent not found: $agentId")

        val entity =
            AgentKnowledgeMapper.toEntity(agent, request).copy(
                status = KnowledgeStatus.PENDING,
                errorMessage = null,
                lastProcessedAt = null,
            )

        val saved = repo.save(entity)

        scheduleIngestionAfterCommit(agentId, saved.id!!)

        return AgentKnowledgeMapper.toResponse(saved)
    }

    // -------------------------------
    // CREATE (File-based)
    // -------------------------------
    @Transactional
    fun createFileKnowledge(
        agentId: UUID,
        request: FileKnowledgeInternalRequest,
    ): AgentKnowledgeResponseDto {
        val agent =
            agentRepo.findByIdOrNull(agentId)
                ?: throw EntityNotFoundException("Agent not found: $agentId")

        val entity =
            AgentKnowledgeMapper.toEntity(agent, request).copy(
                status = KnowledgeStatus.INGESTING,
                errorMessage = null,
                lastProcessedAt = null,
            )

        val saved = repo.save(entity)

        return AgentKnowledgeMapper.toResponse(saved)
    }

    @Transactional
    fun update(
        agentId: UUID,
        knowledgeId: UUID,
        request: AgentKnowledgeRequestDto,
    ): AgentKnowledgeResponseDto {
        val existing =
            repo.findByIdAndAgentId(knowledgeId, agentId)
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")

        val sourceChanged =
            existing.sourceType != request.sourceType ||
                existing.sourceUri != request.sourceUri ||
                existing.metadata != request.metadata

        existing.apply {
            name = request.name
            sourceType = request.sourceType
            sourceUri = request.sourceUri
            metadata = request.metadata
        }

        val saved = repo.save(existing)

        if (sourceChanged) {
            saved.status = KnowledgeStatus.PENDING
            saved.errorMessage = null
            saved.lastProcessedAt = null
            repo.save(saved)

            scheduleIngestionAfterCommit(agentId, saved.id!!)
        }

        return AgentKnowledgeMapper.toResponse(saved)
    }

    @Transactional
    fun softDelete(
        agentId: UUID,
        knowledgeId: UUID,
    ) {
        val existing =
            repo.findByIdAndAgentId(knowledgeId, agentId)
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")

        existing.markDeleted()
        repo.save(existing)
    }

    private fun scheduleIngestionAfterCommit(
        agentId: UUID,
        knowledgeId: UUID,
    ) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    ingestionLifecycleService.triggerIngestionAsync(agentId, knowledgeId)
                }
            },
        )
    }
}

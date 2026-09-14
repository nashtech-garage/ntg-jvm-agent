package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.memory.AgentMemory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface AgentMemoryRepository : JpaRepository<AgentMemory, UUID> {
    fun countByUserId(userId: UUID): Long

    fun findAllByUserIdOrderByTypeAscNameAsc(userId: UUID): List<AgentMemory>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): AgentMemory?

    fun findByUserIdAndAgentIdIsNullAndName(
        userId: UUID,
        name: String,
    ): AgentMemory?

    fun findByUserIdAndAgentIdAndName(
        userId: UUID,
        agentId: UUID,
        name: String,
    ): AgentMemory?

    @Query(
        """
        SELECT m FROM AgentMemory m
        WHERE m.userId = :userId
          AND (m.agentId IS NULL OR m.agentId = :agentId)
        ORDER BY m.type, m.name
        """,
    )
    fun findVisibleForAgent(
        userId: UUID,
        agentId: UUID,
    ): List<AgentMemory>
}

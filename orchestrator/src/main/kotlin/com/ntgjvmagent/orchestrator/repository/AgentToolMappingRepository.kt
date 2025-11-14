package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.mapping.AgentToolMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgentToolMappingRepository : JpaRepository<AgentToolMapping, UUID> {
    fun findByAgentId(agentId: UUID): List<AgentToolMapping>

    fun existsByAgentIdAndToolId(
        agentId: UUID,
        toolId: UUID,
    ): Boolean

    @Modifying
    fun deleteByAgentIdAndToolId(
        agentId: UUID,
        toolId: UUID,
    )
}

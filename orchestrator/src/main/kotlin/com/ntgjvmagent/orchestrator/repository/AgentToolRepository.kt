package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgentToolRepository : JpaRepository<AgentTool, UUID> {
    fun findByAgentId(agentId: UUID): List<AgentTool>

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

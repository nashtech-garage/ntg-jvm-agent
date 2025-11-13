package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.mapping.AgentKnowledgeMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgentKnowledgeMappingRepository : JpaRepository<AgentKnowledgeMapping, UUID> {
    fun findByAgentId(agentId: UUID): List<AgentKnowledgeMapping>

    fun existsByAgentIdAndKnowledgeId(
        agentId: UUID,
        knowledgeId: UUID,
    ): Boolean

    @Modifying
    fun deleteByAgentIdAndKnowledgeId(
        agentId: UUID,
        knowledgeId: UUID,
    )
}

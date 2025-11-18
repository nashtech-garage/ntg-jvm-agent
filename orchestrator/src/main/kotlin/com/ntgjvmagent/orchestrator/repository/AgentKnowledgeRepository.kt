package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgentKnowledgeRepository : JpaRepository<AgentKnowledge, UUID> {
    fun findAllByAgentIdAndActiveTrue(agentId: UUID): List<AgentKnowledge>

    fun findByIdAndAgentId(
        knowledgeId: UUID,
        agentId: UUID,
    ): AgentKnowledge?

    fun existsByIdAndAgentId(
        knowledgeId: UUID,
        agentId: UUID,
    ): Boolean
}

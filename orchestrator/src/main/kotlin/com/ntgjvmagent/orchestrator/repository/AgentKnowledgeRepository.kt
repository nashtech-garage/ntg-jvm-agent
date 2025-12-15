package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgentKnowledgeRepository : JpaRepository<AgentKnowledge, UUID> {
    fun findAllByAgentId(agentId: UUID): List<AgentKnowledge>

    fun findAllByAgentIdAndActiveTrue(agentId: UUID): List<AgentKnowledge>

    fun findAllByStatus(ingesting: KnowledgeStatus): List<AgentKnowledge>

    fun findByIdAndAgentId(
        knowledgeId: UUID,
        agentId: UUID,
    ): AgentKnowledge?

    fun existsByIdAndAgentId(
        knowledgeId: UUID,
        agentId: UUID,
    ): Boolean

    @Modifying
    @Query(
        """
    UPDATE AgentKnowledge ak
    SET ak.status = :status,
        ak.lastProcessedAt = CURRENT_TIMESTAMP,
        ak.errorMessage = :error
    WHERE ak.id = :id
""",
    )
    fun updateStatus(
        @Param("id") id: UUID,
        @Param("status") status: KnowledgeStatus,
        @Param("error") error: String?,
    )
}

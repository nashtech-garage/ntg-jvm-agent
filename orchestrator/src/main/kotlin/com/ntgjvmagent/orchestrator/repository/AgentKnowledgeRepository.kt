package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgentKnowledgeRepository : JpaRepository<AgentKnowledge, UUID> {
    fun findAllByAgentId(agentId: UUID): List<AgentKnowledge>

    /**
     * Aware that we cannot use `findAllByAgentIdAndNameContainingIgnoreCase`
     * because Spring Data does not treat `agentId` as a direct field when
     * combined with another condition.
     * The correct derived query name would be `findAllByAgent_IdAndNameContainingIgnoreCase`,
     * but the underscore violates ktlint rules.
     * Using a custom @Query here.
     */
    @Query(
        """
    SELECT k
    FROM AgentKnowledge k
    WHERE k.agent.id = :agentId
      AND LOWER(k.name) LIKE LOWER(CONCAT('%', :name, '%'))
    """,
    )
    fun searchByAgentAndName(
        agentId: UUID,
        name: String,
    ): List<AgentKnowledge>

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

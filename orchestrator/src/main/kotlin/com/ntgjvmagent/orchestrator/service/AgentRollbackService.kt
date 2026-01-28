package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.hibernate.envers.AuditReaderFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service that rolls an Agent configuration back to a specific revision using Hibernate Envers.
 *
 * Important notes:
 * - Rollback does NOT modify existing revisions.
 * - The system will create a new revision (revtype=MOD) to record the rollback action.
 */
@Service
class AgentRollbackService(
    private val entityManager: EntityManager,
    private val agentRepository: AgentRepository,
) {

    /**
     * Roll back an Agent to the state at [targetRevision].
     *
     * @param agentId Agent ID (UUID)
     * @param targetRevision The revision number to roll back to (e.g. 101)
     * @param includeSecrets Whether to roll back sensitive fields like apiKey (default: false)
     */
    @Transactional
    fun rollbackAgentToRevision(
        agentId: UUID,
        targetRevision: Int,
        includeSecrets: Boolean = false,
    ): Agent {
        require(targetRevision > 0) { "targetRevision must be > 0" }

        val session = entityManager.unwrap(Session::class.java)
        val auditReader = AuditReaderFactory.get(session)

        // 1) Load the snapshot at the target revision
        val snapshot = auditReader.find(Agent::class.java, agentId, targetRevision)
            ?: throw IllegalArgumentException("Audit snapshot not found for agentId=$agentId at rev=$targetRevision")

        // 2) Load the current (managed) entity
        val current = agentRepository.findById(agentId)
            .orElseThrow { IllegalArgumentException("Current Agent not found with id=$agentId") }

        // 3) Copy selected fields back (do not copy id/audit fields)
        applySnapshot(current, snapshot, includeSecrets)

        // 4) Save + flush so Envers creates a new revision
        val saved = agentRepository.save(current)
        entityManager.flush()
        return saved
    }

    private fun applySnapshot(current: Agent, snapshot: Agent, includeSecrets: Boolean) {
        // Business fields
        current.name = snapshot.name
        current.description = snapshot.description
        current.avatar = snapshot.avatar
        current.provider = snapshot.provider
        current.baseUrl = snapshot.baseUrl
        current.chatCompletionsPath = snapshot.chatCompletionsPath
        current.embeddingsPath = snapshot.embeddingsPath
        current.embeddingModel = snapshot.embeddingModel
        current.dimension = snapshot.dimension
        current.model = snapshot.model
        current.temperature = snapshot.temperature
        current.maxTokens = snapshot.maxTokens
        current.topP = snapshot.topP
        current.frequencyPenalty = snapshot.frequencyPenalty
        current.presencePenalty = snapshot.presencePenalty
        current.settings = snapshot.settings

        // Soft delete fields
        current.deletedAt = snapshot.deletedAt
        current.active = snapshot.active

        // Secrets (optional)
        if (includeSecrets) {
            current.apiKey = snapshot.apiKey
        }
    }
}

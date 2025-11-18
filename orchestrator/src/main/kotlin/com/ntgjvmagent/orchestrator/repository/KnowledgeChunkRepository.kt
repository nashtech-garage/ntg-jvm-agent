package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KnowledgeChunkRepository : JpaRepository<KnowledgeChunk, UUID> {
    // Get all chunks for a knowledge source belonging to a specific agent
    fun findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(
        knowledgeId: UUID,
        agentId: UUID,
    ): List<KnowledgeChunk>

    // Count chunks for a knowledge source of a specific agent
    fun countByKnowledgeIdAndKnowledgeAgentId(
        knowledgeId: UUID,
        agentId: UUID,
    ): Long

    // Find a chunk by its ID, knowledge ID, and agent ID
    fun findByIdAndKnowledgeIdAndKnowledgeAgentId(
        chunkId: UUID,
        knowledgeId: UUID,
        agentId: UUID,
    ): KnowledgeChunk?

    // Get the max chunk order for a knowledge source of a specific agent
    @Query(
        """
        SELECT MAX(c.chunkOrder)
        FROM KnowledgeChunk c
        WHERE c.knowledge.id = :knowledgeId AND c.knowledge.agent.id = :agentId
    """,
    )
    fun findMaxChunkOrderByKnowledgeIdAndAgentId(
        knowledgeId: UUID,
        agentId: UUID,
    ): Int?

    // Get IDs of all active knowledge for a specific agent
    @Query(
        """
        SELECT c.knowledge.id
        FROM KnowledgeChunk c
        WHERE c.knowledge.active = true AND c.knowledge.agent.id = :agentId
    """,
    )
    fun findAllKnowledgeIdsActiveByAgent(agentId: UUID): List<UUID>
}

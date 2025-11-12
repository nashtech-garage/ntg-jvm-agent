package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KnowledgeChunkRepository : JpaRepository<KnowledgeChunk, UUID> {
    fun findAllByKnowledgeId(knowledgeId: UUID): List<KnowledgeChunk>

    fun countByKnowledgeId(knowledgeId: UUID): Long

    /**
     * Custom query to get IDs of all chunks whose knowledge is active
     */
    @Query(
        """
        SELECT c.knowledge.id
        FROM KnowledgeChunk c
        WHERE c.knowledge.active = true
    """,
    )
    fun findAllKnowledgeIdsActive(): List<UUID>
}

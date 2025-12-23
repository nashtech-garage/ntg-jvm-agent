package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.embedding.job.EmbeddingJobStatus
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface EmbeddingJobRepository : JpaRepository<EmbeddingJob, UUID> {
    @Query(
        """
    SELECT id FROM embedding_job
    WHERE status = CAST(:status AS VARCHAR)
      AND (next_attempt_at IS NULL OR next_attempt_at <= NOW())
    ORDER BY created_at
    FOR UPDATE SKIP LOCKED
""",
        nativeQuery = true,
    )
    fun lockNextJobId(
        @Param("status") status: String,
    ): List<UUID>

    @Query(
        """
    SELECT j
    FROM EmbeddingJob j
    JOIN FETCH j.chunk
    JOIN FETCH j.agent
    JOIN FETCH j.knowledge
    WHERE j.id = :id
""",
    )
    fun findByIdWithFetches(
        @Param("id") id: UUID,
    ): EmbeddingJob?

    fun findAllByStatus(status: EmbeddingJobStatus): List<EmbeddingJob>

    @Query(
        """
    SELECT COUNT(j)
    FROM EmbeddingJob j
    WHERE j.knowledge.id = :knowledgeId
      AND j.status IN (:statuses)
    """,
    )
    fun countUnfinished(
        @Param("knowledgeId") knowledgeId: UUID,
        @Param("statuses") statuses: List<EmbeddingJobStatus> =
            listOf(
                EmbeddingJobStatus.PENDING,
                EmbeddingJobStatus.RUNNING,
            ),
    ): Long

    @Query(
        """
    SELECT COUNT(j)
    FROM EmbeddingJob j
    WHERE j.knowledge.id = :knowledgeId
      AND j.status = :status
    """,
    )
    fun countFailed(
        @Param("knowledgeId") knowledgeId: UUID,
        @Param("status") status: EmbeddingJobStatus = EmbeddingJobStatus.FAILED,
    ): Long

    @Modifying
    @Transactional
    @Query(
        """
    DELETE FROM EmbeddingJob j
    WHERE j.chunk.id IN :chunkIds
    """,
    )
    fun deleteAllByChunkIds(
        @Param("chunkIds") chunkIds: List<UUID>,
    )

    fun findAllByKnowledgeIdAndKnowledgeAgentIdOrderByCreatedAtAsc(
        knowledgeId: UUID,
        agentId: UUID,
    ): List<EmbeddingJob>
}

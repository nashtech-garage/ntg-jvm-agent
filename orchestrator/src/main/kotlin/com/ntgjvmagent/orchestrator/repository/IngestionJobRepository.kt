package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.IngestionJob
import com.ntgjvmagent.orchestrator.ingestion.job.IngestionJobStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface IngestionJobRepository : JpaRepository<IngestionJob, UUID> {
    @Query(
        """
    SELECT j
    FROM IngestionJob j
    JOIN FETCH j.knowledge k
    JOIN FETCH k.agent a
    WHERE j.status = :status
    ORDER BY j.createdAt ASC
""",
    )
    fun findFirstWithFetch(
        @Param("status") status: IngestionJobStatus,
    ): IngestionJob?

    fun findAllByStatus(status: IngestionJobStatus): List<IngestionJob>
}

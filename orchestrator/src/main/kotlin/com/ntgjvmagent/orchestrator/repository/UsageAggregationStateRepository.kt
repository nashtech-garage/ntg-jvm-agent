package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.UsageAggregationState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface UsageAggregationStateRepository : JpaRepository<UsageAggregationState, Short> {
    @Query(
        """
        SELECT s.lastProcessedDate
        FROM UsageAggregationState s
        WHERE s.id = 1
        """,
    )
    fun getLastProcessedDate(): LocalDate

    @Modifying
    @Query(
        """
        UPDATE UsageAggregationState s
        SET s.lastProcessedDate = :date,
            s.updatedAt = CURRENT_TIMESTAMP
        WHERE s.id = 1
        """,
    )
    fun updateLastProcessedDate(
        @Param("date") date: LocalDate,
    )
}

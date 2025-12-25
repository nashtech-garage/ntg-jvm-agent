package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.TokenUsageLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface TokenUsageLogRepository : JpaRepository<TokenUsageLog, UUID> {
    @Query(
        """
        SELECT COALESCE(SUM(t.totalTokens), 0)
        FROM TokenUsageLog t
        WHERE t.userId = :userId
          AND t.createdAt >= :from
          AND t.createdAt < :to
        """,
    )
    fun sumDailyTokens(
        userId: UUID,
        from: Instant,
        to: Instant,
    ): Long
}

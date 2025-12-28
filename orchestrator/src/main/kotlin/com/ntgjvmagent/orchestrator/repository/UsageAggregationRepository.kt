package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.TokenUsageDaily
import com.ntgjvmagent.orchestrator.usage.aggregate.projection.UsageByAgentProjection
import com.ntgjvmagent.orchestrator.usage.aggregate.projection.UsageByUserProjection
import com.ntgjvmagent.orchestrator.usage.aggregate.projection.UsageDailyTimeSeriesProjection
import com.ntgjvmagent.orchestrator.usage.aggregate.projection.UsageSumProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface UsageAggregationRepository : JpaRepository<TokenUsageDaily, Long> {
    @Modifying
    @Query(
        """
        INSERT INTO token_usage_daily (
            usage_date,
            agent_id,
            user_id,
            provider,
            operation,
            model,
            prompt_tokens,
            completion_tokens,
            total_tokens,
            updated_at
        )
        SELECT
            usage_date,
            agent_id,
            user_id,
            provider,
            operation,
            model,
            SUM(prompt_tokens),
            SUM(completion_tokens),
            SUM(total_tokens),
            now()
        FROM token_usage_log
        WHERE usage_date = :day
        GROUP BY
            usage_date,
            agent_id,
            user_id,
            provider,
            operation,
            model
        ON CONFLICT (
            usage_date,
            agent_id,
            user_id,
            provider,
            operation,
            model
        )
        DO UPDATE SET
            prompt_tokens     = EXCLUDED.prompt_tokens,
            completion_tokens = EXCLUDED.completion_tokens,
            total_tokens      = EXCLUDED.total_tokens,
            updated_at        = now()
        """,
        nativeQuery = true,
    )
    fun recomputeDay(
        @Param("day") day: LocalDate,
    ): Int

    @Query(
        """
        SELECT
            COALESCE(SUM(t.totalTokens), 0) AS totalTokens,
            COALESCE(SUM(t.promptTokens), 0) AS promptTokens,
            COALESCE(SUM(t.completionTokens), 0) AS completionTokens
        FROM TokenUsageDaily t
        WHERE t.usageDate BETWEEN :from AND :to
        """,
    )
    fun sumUsage(
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): UsageSumProjection

    @Query(
        """
    SELECT
        t.usageDate AS usageDate,
        COALESCE(SUM(t.totalTokens), 0) AS totalTokens,
        COALESCE(SUM(t.promptTokens), 0) AS promptTokens,
        COALESCE(SUM(t.completionTokens), 0) AS completionTokens
    FROM TokenUsageDaily t
    WHERE t.usageDate BETWEEN :from AND :to
    GROUP BY t.usageDate
    ORDER BY t.usageDate
    """,
    )
    fun aggregateDaily(
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<UsageDailyTimeSeriesProjection>

    @Query(
        """
    SELECT
        a.id AS agentId,
        a.name AS agentName,
        COALESCE(SUM(t.promptTokens), 0) AS promptTokens,
        COALESCE(SUM(t.completionTokens), 0) AS completionTokens,
        COALESCE(SUM(t.totalTokens), 0) AS totalTokens
    FROM TokenUsageDaily t
    JOIN Agent a ON a.id = t.agentId
    WHERE t.usageDate BETWEEN :from AND :to
    GROUP BY a.id, a.name
    ORDER BY SUM(t.totalTokens) DESC
    """,
    )
    fun aggregateByAgent(
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<UsageByAgentProjection>

    @Query(
        """
    SELECT
        t.userId AS userId,
        u.name AS userName,
        COALESCE(SUM(t.promptTokens), 0) AS promptTokens,
        COALESCE(SUM(t.completionTokens), 0) AS completionTokens,
        COALESCE(SUM(t.totalTokens), 0) AS totalTokens
    FROM TokenUsageDaily t
    LEFT JOIN User u ON u.id = t.userId
    WHERE t.usageDate BETWEEN :from AND :to
    GROUP BY t.userId, u.name
    ORDER BY SUM(t.totalTokens) DESC
    """,
    )
    fun aggregateByUser(
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<UsageByUserProjection>

    @Query(
        """
        SELECT MAX(t.usageDate)
        FROM TokenUsageDaily t
        """,
    )
    fun findLatestAggregatedDate(): LocalDate?
}

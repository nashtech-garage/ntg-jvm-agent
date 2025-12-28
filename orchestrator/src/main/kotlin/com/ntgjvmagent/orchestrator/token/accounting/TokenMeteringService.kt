package com.ntgjvmagent.orchestrator.token.accounting

import com.ntgjvmagent.orchestrator.entity.TokenUsageLog
import com.ntgjvmagent.orchestrator.exception.TokenLimitExceededException
import com.ntgjvmagent.orchestrator.exception.TokenQuotaUnavailableException
import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.repository.TokenUsageLogRepository
import com.ntgjvmagent.orchestrator.repository.UserTokenQuotaRepository
import com.ntgjvmagent.orchestrator.service.DynamicModelService
import com.ntgjvmagent.orchestrator.token.cache.DailyTokenUsageCache
import com.ntgjvmagent.orchestrator.utils.toNormalizedUsage
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.metadata.Usage
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Suppress("TooGenericExceptionCaught")
@Service
class TokenMeteringService(
    private val tokenUsageLogRepository: TokenUsageLogRepository,
    private val userTokenQuotaRepository: UserTokenQuotaRepository,
    private val dailyTokenUsageCache: DailyTokenUsageCache,
    private val dynamicModelService: DynamicModelService,
    private val meterRegistry: MeterRegistry,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /* =========================================================
     * QUOTA ENFORCEMENT (HARD)
     * ========================================================= */

    fun assertWithinBudget(
        userId: UUID,
        estimatedInputTokens: Int,
    ) {
        val budget = getBudget(userId)

        if (estimatedInputTokens > budget.remaining) {
            throw TokenLimitExceededException(
                "Token limit exceeded. Required=$estimatedInputTokens, Remaining=${budget.remaining}",
            )
        }
    }

    fun getBudget(userId: UUID): TokenBudget {
        val today = LocalDate.now(ZoneOffset.UTC)

        val quota =
            userTokenQuotaRepository
                .findByUserId(userId)
                ?.dailyLimit
                ?: DEFAULT_DAILY_LIMIT

        dailyTokenUsageCache.getUsedTokens(userId, today)?.let {
            return TokenBudget(limit = quota, used = it)
        }

        val from =
            today
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)

        val to =
            today
                .plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)

        val usedFromDb =
            tokenUsageLogRepository
                .sumDailyTokens(userId, from, to)

        // Populate cache (this MUST succeed for enforcement correctness)
        try {
            dailyTokenUsageCache.setUsedTokens(
                userId = userId,
                date = today,
                value = usedFromDb,
                ttlSeconds = secondsUntilEndOfDayUtc(),
            )
        } catch (ex: Exception) {
            logger.error(
                "Quota cache population failed — enforcement cannot proceed safely",
                ex,
            )
            throw TokenQuotaUnavailableException(cause = ex)
        }

        return TokenBudget(limit = quota, used = usedFromDb)
    }

    /* =========================================================
     * ACCOUNTING (BEST-EFFORT)
     * ========================================================= */

    /**
     * Best-effort token accounting.
     * MUST NOT block user execution.
     */
    fun record(
        userId: UUID?,
        agentId: UUID,
        operation: TokenOperation,
        usage: Usage,
        toolName: String? = null,
        correlationId: String? = null,
    ) {
        if (userId == null) return

        if (usage.totalTokens <= 0) return

        try {
            val normalized = usage.toNormalizedUsage()
            val agentConfig = dynamicModelService.getAgentConfig(agentId)

            tokenUsageLogRepository.save(
                TokenUsageLog(
                    userId = userId,
                    agentId = agentId,
                    organizationId = null,
                    provider = agentConfig.provider,
                    model = agentConfig.model,
                    operation = operation,
                    toolName = toolName,
                    promptTokens = normalized.promptTokens,
                    completionTokens = normalized.completionTokens,
                    totalTokens = normalized.totalTokens,
                    correlationId = correlationId,
                ),
            )

            // keep cache in sync (best-effort)
            incrementDailyUsageBestEffort(userId, usage.totalTokens.toLong())
        } catch (ex: Exception) {
            handleAccountingFailure(ex, correlationId)
        }
    }

    fun recordEstimated(
        userId: UUID?,
        agentId: UUID,
        operation: TokenOperation,
        estimatedPromptTokens: Int,
        estimatedCompletionTokens: Int,
        correlationId: String?,
    ) {
        if (userId == null) return

        val promptTokens = estimatedPromptTokens.toLong().coerceAtLeast(0L)
        val completionTokens = estimatedCompletionTokens.toLong().coerceAtLeast(0L)
        val total = promptTokens + completionTokens
        if (total <= 0) return

        try {
            tokenUsageLogRepository.save(
                TokenUsageLog(
                    userId = userId,
                    agentId = agentId,
                    organizationId = null,
                    provider = "ESTIMATED",
                    model = "ESTIMATED",
                    operation = operation,
                    toolName = null,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = total,
                    correlationId = correlationId,
                ),
            )

            incrementDailyUsageBestEffort(userId, total)
        } catch (ex: Exception) {
            handleAccountingFailure(ex, correlationId)
        }
    }

    /* =========================================================
     * INTERNAL HELPERS
     * ========================================================= */

    private fun incrementDailyUsageBestEffort(
        userId: UUID,
        delta: Long,
    ) {
        try {
            dailyTokenUsageCache.incrementUsedTokens(
                userId,
                LocalDate.now(ZoneOffset.UTC),
                delta,
            )
        } catch (ex: Exception) {
            // quota cache drift is acceptable short-term
            logger.error(
                "Quota cache update failed (best-effort). userId={}",
                userId,
                ex,
            )
            meterRegistry
                .counter("token.accounting.cache.failure")
                .increment()
        }
    }

    private fun handleAccountingFailure(
        ex: Exception,
        correlationId: String?,
    ) {
        logger.error(
            "Best-effort token accounting failed. correlationId={}",
            correlationId,
            ex,
        )

        meterRegistry
            .counter("token.accounting.failure")
            .increment()
    }

    fun secondsUntilEndOfDayUtc(): Long {
        val now = Instant.now()
        val endOfDay =
            LocalDate
                .now(ZoneOffset.UTC)
                .plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)

        return Duration
            .between(now, endOfDay)
            .seconds
            .coerceAtLeast(MIN_CACHE_TTL_SECONDS)
    }

    companion object {
        private const val DEFAULT_DAILY_LIMIT = 100_000L
        private const val MIN_CACHE_TTL_SECONDS = 60L
    }
}

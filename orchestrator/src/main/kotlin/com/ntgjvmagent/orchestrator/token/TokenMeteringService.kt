package com.ntgjvmagent.orchestrator.token

import com.ntgjvmagent.orchestrator.entity.TokenUsageLog
import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.repository.TokenUsageLogRepository
import com.ntgjvmagent.orchestrator.repository.UserTokenQuotaRepository
import com.ntgjvmagent.orchestrator.service.DynamicModelService
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.metadata.Usage
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Service
class TokenMeteringService(
    private val tokenUsageLogRepository: TokenUsageLogRepository,
    private val userTokenQuotaRepository: UserTokenQuotaRepository,
    private val dailyTokenUsageCache: DailyTokenUsageCache,
    private val dynamicModelService: DynamicModelService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getBudget(userId: UUID): TokenBudget {
        val today = LocalDate.now(ZoneOffset.UTC)

        val quota =
            userTokenQuotaRepository
                .findByUserId(userId)
                ?.dailyLimit
                ?: DEFAULT_DAILY_LIMIT

        // Try cache first
        val cachedUsed = dailyTokenUsageCache.getUsedTokens(userId, today)
        if (cachedUsed != null) {
            return TokenBudget(
                limit = quota,
                used = cachedUsed,
            )
        }

        // Fallback to DB
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
                .sumDailyTokens(
                    userId = userId,
                    from = from,
                    to = to,
                )

        // Populate cache
        dailyTokenUsageCache.setUsedTokens(
            userId = userId,
            date = today,
            value = usedFromDb,
            ttlSeconds = secondsUntilEndOfDayUtc(),
        )

        return TokenBudget(
            limit = quota,
            used = usedFromDb,
        )
    }

    /**
     * MUST be called whenever tokens are recorded
     */
    fun incrementDailyUsage(
        userId: UUID?,
        delta: Long,
    ) {
        if (userId == null) {
            // system-level usage → do nothing
            return
        }

        incrementUserDailyUsage(userId, delta)
    }

    fun incrementUserDailyUsage(
        userId: UUID,
        delta: Long,
    ) {
        val today = LocalDate.now(ZoneOffset.UTC)
        dailyTokenUsageCache.incrementUsedTokens(userId, today, delta)
    }

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

    fun record(
        userId: UUID,
        agentId: UUID,
        operation: TokenOperation,
        usage: Usage,
        toolName: String? = null,
        correlationId: String? = null,
    ) {
        runCatching {
            // Guard against empty usage
            if (usage.promptTokens == 0 &&
                usage.completionTokens == 0 &&
                usage.totalTokens == 0
            ) {
                return
            }

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
                    promptTokens = usage.promptTokens,
                    completionTokens = usage.completionTokens,
                    totalTokens = usage.totalTokens,
                    correlationId = correlationId,
                ),
            )

            // KEEP CACHE IN SYNC
            incrementDailyUsage(userId, usage.totalTokens.toLong())
        }.onFailure {
            logger.warn("Token accounting failed", it)
        }
    }

    fun recordEstimated(
        userId: UUID?,
        agentId: UUID,
        operation: TokenOperation,
        estimatedTokens: Int,
        correlationId: String?,
    ) {
        runCatching {
            if (estimatedTokens <= 0) return

            val log =
                TokenUsageLog(
                    userId = userId,
                    agentId = agentId,
                    organizationId = null,
                    provider = "ESTIMATED",
                    model = "ESTIMATED",
                    operation = operation,
                    toolName = null,
                    promptTokens = 0,
                    completionTokens = estimatedTokens,
                    totalTokens = estimatedTokens,
                    correlationId = correlationId,
                )

            tokenUsageLogRepository.save(log)

            // KEEP CACHE IN SYNC
            incrementDailyUsage(userId, estimatedTokens.toLong())
        }.onFailure {
            logger.warn("Token accounting failed", it)
        }
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
            .coerceAtLeast(MIN_CACHE_TTL_SECONDS) // safety buffer
    }

    companion object {
        private const val DEFAULT_DAILY_LIMIT = 100_000L
        private const val MIN_CACHE_TTL_SECONDS = 60L
    }
}

package com.ntgjvmagent.orchestrator.embedding

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingTierConfig
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.service.DynamicModelService
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import org.slf4j.LoggerFactory
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

@Profile("!test")
@Service
class EmbeddingService(
    private val dynamicModelService: DynamicModelService,
    private val agentRepo: AgentRepository,
    private val tierCache: EmbeddingTierConfigCacheService,
) {
    companion object {
        private const val DEFAULT_TIER = "free"
        private const val LIMITER_TIMEOUT_SECONDS = 5L
        private const val JITTER_DIVISOR = 10L
        private const val SECONDS_TO_MILLIS = 1000L
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }

    private val rateLimiters = ConcurrentHashMap<String, RateLimiter>()
    private val log = LoggerFactory.getLogger(EmbeddingService::class.java)

    fun embed(
        agentId: UUID,
        text: String,
    ): FloatArray = execute(agentId) { it.embed(text) }

    fun embedBatch(
        agentId: UUID,
        texts: List<String>,
    ): List<FloatArray> =
        if (texts.isEmpty()) {
            emptyList()
        } else {
            execute(agentId) { it.embed(texts) }
        }

    private fun <T> execute(
        agentId: UUID,
        block: (EmbeddingModel) -> T,
    ): T {
        val tier = resolveTier(agentId)
        val cfg = tierCache[tier]
        val limiter = limiterFor(agentId, tier, cfg)
        val model = dynamicModelService.getEmbeddingModel(agentId)

        var attempt = 0

        while (true) {
            attempt++
            RateLimiter.waitForPermission(limiter)

            try {
                return block(model)
            } catch (ex: HttpClientErrorException.TooManyRequests) {
                retryOrThrow(agentId, tier, attempt, cfg, ex)
            } catch (ex: WebClientResponseException) {
                if (ex.statusCode.value() == HTTP_TOO_MANY_REQUESTS) {
                    retryOrThrow(agentId, tier, attempt, cfg, ex)
                } else {
                    throw ex
                }
            }
        }
    }

    private fun resolveTier(agentId: UUID): String {
        val agent = agentRepo.findById(agentId).orElseThrow()
        return ((agent.settings?.get("tier") ?: DEFAULT_TIER).toString()).lowercase()
    }

    private fun limiterFor(
        agentId: UUID,
        tier: String,
        cfg: EmbeddingTierConfig,
    ): RateLimiter =
        rateLimiters.computeIfAbsent("$agentId:$tier") {
            RateLimiter.of(
                "$agentId-$tier",
                RateLimiterConfig
                    .custom()
                    .limitRefreshPeriod(Duration.ofSeconds(cfg.intervalSeconds))
                    .limitForPeriod(cfg.maxCallsPerInterval)
                    .timeoutDuration(Duration.ofSeconds(LIMITER_TIMEOUT_SECONDS))
                    .build(),
            )
        }

    private fun retryOrThrow(
        agentId: UUID,
        tier: String,
        attempt: Int,
        cfg: EmbeddingTierConfig,
        ex: WebClientResponseException,
    ) {
        if (attempt >= cfg.maxRetries) {
            log.warn("Embedding failed after {} retries: {}", attempt, ex.message)
            throw ex
        }

        val backoff = computeBackoff(cfg, attempt, parseRetryAfter(ex))
        log.warn("429 TooManyRequests agent={} tier={}, waiting {} ms...", agentId, tier, backoff)
        Thread.sleep(backoff)
    }

    private fun retryOrThrow(
        agentId: UUID,
        tier: String,
        attempt: Int,
        cfg: EmbeddingTierConfig,
        ex: HttpClientErrorException.TooManyRequests,
    ) {
        if (attempt >= cfg.maxRetries) {
            log.warn("Embedding failed after {} retries: {}", attempt, ex.message)
            throw ex
        }

        val backoff = computeBackoff(cfg, attempt, parseRetryAfter(ex))
        log.warn("429 TooManyRequests agent={} tier={}, waiting {} ms...", agentId, tier, backoff)
        Thread.sleep(backoff)
    }

    private fun parseRetryAfter(ex: WebClientResponseException): Long? =
        ex.headers
            .getFirst("Retry-After")
            ?.toLongOrNull()
            ?.times(SECONDS_TO_MILLIS)

    private fun parseRetryAfter(ex: HttpClientErrorException.TooManyRequests): Long? =
        ex.responseHeaders
            ?.getFirst("Retry-After")
            ?.toLongOrNull()
            ?.times(SECONDS_TO_MILLIS)

    // --------------------------------------------------------------------
    // Backoff with cap + jitter
    // --------------------------------------------------------------------
    private fun computeBackoff(
        cfg: EmbeddingTierConfig,
        attempt: Int,
        retryAfterMs: Long?,
    ): Long {
        val exponential = cfg.baseBackoffMs * (1L shl (attempt - 1))
        val capped =
            retryAfterMs?.coerceAtMost(cfg.maxBackoffMs)
                ?: exponential.coerceAtMost(cfg.maxBackoffMs)

        val jitter = capped / JITTER_DIVISOR
        return capped + ThreadLocalRandom.current().nextLong(-jitter, jitter)
    }
}

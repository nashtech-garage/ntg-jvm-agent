package com.ntgjvmagent.orchestrator.embedding

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingTierConfig
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.service.DynamicModelService
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

@Service
class EmbeddingService(
    private val dynamicModelService: DynamicModelService,
    private val agentRepo: AgentRepository,
    private val tierCache: EmbeddingTierConfigCacheService,
) {
    companion object {
        private const val DEFAULT_TIER = "free"

        private const val RETRY_MULTIPLIER = 2.0
        private const val RETRY_JITTER = 0.1

        private const val MILLIS_PER_SECOND = 1000L
        private const val MIN_DELAY_MS = 0L

        private const val STATUS_TOO_MANY_REQUESTS = 429
        private const val STATUS_UNAUTHORIZED = 401

        private val RATE_LIMITER_TIMEOUT = Duration.ofSeconds(2)
        private val BLOCKING_TIMEOUT = Duration.ofSeconds(30)
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val rateLimiters = ConcurrentHashMap<String, RateLimiter>()

    // ---------------------------------------------------------------------
    // PUBLIC API (sync façade)
    // ---------------------------------------------------------------------

    fun embed(
        agentId: UUID,
        text: String,
    ): FloatArray =
        embedReactive(agentId, text)
            .publishOn(Schedulers.boundedElastic())
            .block(BLOCKING_TIMEOUT)
            ?: error("Embedding timed out for agent $agentId")

    fun embedBatch(
        agentId: UUID,
        texts: List<String>,
    ): List<FloatArray> {
        if (texts.isEmpty()) return emptyList()

        return embedBatchReactive(agentId, texts)
            .publishOn(Schedulers.boundedElastic())
            .block(BLOCKING_TIMEOUT)
            ?: error("Batch embedding timed out for agent $agentId")
    }

    // ---------------------------------------------------------------------
    // PUBLIC REACTIVE API (for reactive worker)
    // ---------------------------------------------------------------------

    fun embedReactive(
        agentId: UUID,
        text: String,
    ): Mono<FloatArray> = performReactive(agentId) { model -> model.embedReactive(text) }

    fun embedBatchReactive(
        agentId: UUID,
        texts: List<String>,
    ): Mono<List<FloatArray>> = performReactive(agentId) { model -> model.embedBatchReactive(texts) }

    // ---------------------------------------------------------------------
    // Core reactive pipeline
    // ---------------------------------------------------------------------

    private fun <T : Any> performReactive(
        agentId: UUID,
        block: (ReactiveEmbeddingModel) -> Mono<T>,
    ): Mono<T> {
        val tier = resolveTier(agentId)
        val cfg = tierCache[tier]
        val model = dynamicModelService.getEmbeddingModel(agentId)

        val rateLimiter = createOrGetRateLimiter(agentId, tier, cfg)
        val rateLimiterOp = RateLimiterOperator.of<T>(rateLimiter)

        val fallbackBackoffFn =
            IntervalFunction.ofExponentialRandomBackoff(
                cfg.baseBackoffMs,
                RETRY_MULTIPLIER,
                RETRY_JITTER,
                cfg.maxBackoffMs,
            )

        val lastExceptionRef = AtomicReference<Throwable?>(null)

        val retryConfig =
            RetryConfig
                .custom<T>()
                .maxAttempts(cfg.maxRetries)
                .retryOnException { ex ->
                    lastExceptionRef.set(ex)
                    shouldRetry(ex)
                }.intervalFunction { attempt ->
                    val lastEx = lastExceptionRef.get()
                    val fallbackMs = fallbackBackoffFn.apply(attempt)

                    computeBackoffDelay(
                        attempt = attempt,
                        throwable = lastEx,
                        fallbackBackoffMs = fallbackMs,
                        agentId = agentId,
                        tier = tier,
                    )
                }.build()

        val retryOp = RetryOperator.of<T>(Retry.of("embedding-$agentId", retryConfig))

        return block(model)
            .transformDeferred { it.transform(rateLimiterOp) }
            .transformDeferred { it.transform(retryOp) }
            .doOnError { ex ->
                log.warn(
                    "Embedding failed: agent={} tier={} type={} message={}",
                    agentId,
                    tier,
                    ex.javaClass.simpleName,
                    ex.message,
                )
            }
    }

    // ---------------------------------------------------------------------
    // Retry decision logic
    // ---------------------------------------------------------------------

    private fun shouldRetry(ex: Throwable): Boolean =
        when (ex) {
            is RequestNotPermitted -> true
            is IOException -> true

            is WebClientResponseException -> {
                val status = ex.statusCode.value()
                status == STATUS_TOO_MANY_REQUESTS ||
                    status == STATUS_UNAUTHORIZED ||
                    ex.statusCode.is5xxServerError
            }

            is HttpClientErrorException.TooManyRequests -> true
            is HttpClientErrorException.Unauthorized -> true
            else -> false
        }

    // ---------------------------------------------------------------------
    // Backoff delay calculation
    // ---------------------------------------------------------------------

    private fun computeBackoffDelay(
        attempt: Int,
        throwable: Throwable?,
        fallbackBackoffMs: Long,
        agentId: UUID,
        tier: String,
    ): Long {
        val retryAfterMs =
            when (throwable) {
                is WebClientResponseException ->
                    parseRetryAfterHeader(throwable.headers.getFirst("Retry-After"))
                is HttpClientErrorException ->
                    parseRetryAfterHeader(throwable.responseHeaders?.getFirst("Retry-After"))
                else -> null
            }

        val finalDelay = retryAfterMs ?: fallbackBackoffMs

        if (retryAfterMs != null) {
            log.warn(
                "Retry-After applied: agent={} tier={} attempt={} delay={}ms throwable={}",
                agentId,
                tier,
                attempt,
                finalDelay,
                throwable?.javaClass?.simpleName,
            )
        }

        return finalDelay
    }

    // ---------------------------------------------------------------------
    // Retry-After parsing (lint-safe)
    // ---------------------------------------------------------------------

    private fun parseRetryAfterHeader(header: String?): Long? =
        when {
            header == null -> null

            header.toLongOrNull() != null ->
                header.toLong() * MILLIS_PER_SECOND

            else ->
                try {
                    val instant = ZonedDateTime.parse(header).toInstant()
                    val delayMs = Duration.between(Instant.now(), instant).toMillis()
                    max(delayMs, MIN_DELAY_MS)
                } catch (_: Exception) {
                    null
                }
        }

    // ---------------------------------------------------------------------
    // RateLimiter per (agent + tier)
    // ---------------------------------------------------------------------

    private fun createOrGetRateLimiter(
        agentId: UUID,
        tier: String,
        cfg: EmbeddingTierConfig,
    ): RateLimiter {
        val key = "$agentId:$tier"

        return rateLimiters.computeIfAbsent(key) {
            val config =
                RateLimiterConfig
                    .custom()
                    .limitForPeriod(cfg.maxCallsPerInterval)
                    .limitRefreshPeriod(Duration.ofSeconds(cfg.intervalSeconds))
                    .timeoutDuration(RATE_LIMITER_TIMEOUT)
                    .build()

            RateLimiter.of(key, config)
        }
    }

    // ---------------------------------------------------------------------
    // Tier resolution
    // ---------------------------------------------------------------------

    private fun resolveTier(agentId: UUID): String {
        val agent = agentRepo.findById(agentId).orElseThrow()
        return (agent.settings?.get("tier") ?: DEFAULT_TIER).toString().lowercase()
    }
}

package com.ntgjvmagent.orchestrator.embedding.accounting

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingTierConfig
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

@Component
class EmbeddingRateLimiter {
    private val rateLimiters = ConcurrentHashMap<String, RateLimiter>()

    private companion object {
        private const val RETRY_MULTIPLIER = 2.0
        private const val RETRY_JITTER = 0.1
        private const val MILLIS_PER_SECOND = 1000L
        private const val MIN_DELAY_MS = 0L

        private val RATE_LIMITER_TIMEOUT = Duration.ofSeconds(2)

        private const val STATUS_TOO_MANY_REQUESTS = 429
        private const val STATUS_UNAUTHORIZED = 401
    }

    fun <T : Any> apply(
        agentId: UUID,
        tier: String,
        cfg: EmbeddingTierConfig,
        mono: Mono<T>,
    ): Mono<T> {
        val rateLimiter =
            rateLimiters.computeIfAbsent("$agentId:$tier") {
                RateLimiter.of(
                    "$agentId:$tier",
                    RateLimiterConfig
                        .custom()
                        .limitForPeriod(cfg.maxCallsPerInterval)
                        .limitRefreshPeriod(Duration.ofSeconds(cfg.intervalSeconds))
                        .timeoutDuration(RATE_LIMITER_TIMEOUT)
                        .build(),
                )
            }

        val lastExceptionRef = AtomicReference<Throwable?>(null)

        val retryConfig =
            RetryConfig
                .custom<T>()
                .maxAttempts(cfg.maxRetries)
                .retryOnException {
                    lastExceptionRef.set(it)
                    shouldRetry(it)
                }.intervalFunction { attempt ->
                    val fallbackMs =
                        IntervalFunction
                            .ofExponentialRandomBackoff(
                                cfg.baseBackoffMs,
                                RETRY_MULTIPLIER,
                                RETRY_JITTER,
                                cfg.maxBackoffMs,
                            ).apply(attempt)

                    computeBackoffDelay(
                        lastExceptionRef.get(),
                        fallbackMs,
                    )
                }.build()

        return mono
            .transformDeferred { it.transform(RateLimiterOperator.of(rateLimiter)) }
            .transformDeferred { it.transform(RetryOperator.of(Retry.of("embedding-$agentId", retryConfig))) }
    }

    private fun shouldRetry(ex: Throwable): Boolean =
        when (ex) {
            is RequestNotPermitted -> true
            is IOException -> true
            is WebClientResponseException ->
                ex.statusCode.value() == STATUS_TOO_MANY_REQUESTS ||
                    ex.statusCode.value() == STATUS_UNAUTHORIZED ||
                    ex.statusCode.is5xxServerError
            is HttpClientErrorException.TooManyRequests -> true
            is HttpClientErrorException.Unauthorized -> true
            else -> false
        }

    private fun computeBackoffDelay(
        throwable: Throwable?,
        fallbackMs: Long,
    ): Long =
        when (throwable) {
            is WebClientResponseException ->
                parseRetryAfterHeader(throwable.headers.getFirst("Retry-After"))
            is HttpClientErrorException ->
                parseRetryAfterHeader(throwable.responseHeaders?.getFirst("Retry-After"))
            else -> null
        } ?: fallbackMs

    private fun parseRetryAfterHeader(header: String?): Long? =
        when {
            header == null -> null
            header.toLongOrNull() != null -> header.toLong() * MILLIS_PER_SECOND
            else ->
                try {
                    max(
                        Duration
                            .between(
                                Instant.now(),
                                ZonedDateTime.parse(header).toInstant(),
                            ).toMillis(),
                        MIN_DELAY_MS,
                    )
                } catch (_: Exception) {
                    null
                }
        }
}

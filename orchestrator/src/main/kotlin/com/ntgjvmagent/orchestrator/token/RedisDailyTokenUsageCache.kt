package com.ntgjvmagent.orchestrator.token

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class RedisDailyTokenUsageCache(
    private val redisTemplate: StringRedisTemplate,
) : DailyTokenUsageCache {
    override fun getUsedTokens(
        userId: UUID,
        date: LocalDate,
    ): Long? {
        val key = key(userId, date)
        return redisTemplate.opsForValue().get(key)?.toLong()
    }

    override fun incrementUsedTokens(
        userId: UUID,
        date: LocalDate,
        delta: Long,
    ) {
        val key = key(userId, date)

        val valueOps = redisTemplate.opsForValue()
        val newValue = valueOps.increment(key, delta)

        // If key was just created, set TTL
        if (newValue == delta) {
            val ttl = durationUntilEndOfDay(date)
            redisTemplate.expire(key, ttl)
        }
    }

    override fun setUsedTokens(
        userId: UUID,
        date: LocalDate,
        value: Long,
        ttlSeconds: Long,
    ) {
        val key = key(userId, date)
        redisTemplate.opsForValue().set(key, value.toString(), ttlSeconds, TimeUnit.SECONDS)
    }

    private fun key(
        userId: UUID,
        date: LocalDate,
    ): String = "token:used:$userId:$date"

    private fun durationUntilEndOfDay(date: LocalDate): Duration {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val endOfDay =
            date
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)

        return Duration.between(now, endOfDay)
    }
}

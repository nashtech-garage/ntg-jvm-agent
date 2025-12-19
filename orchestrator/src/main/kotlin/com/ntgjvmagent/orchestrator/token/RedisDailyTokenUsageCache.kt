package com.ntgjvmagent.orchestrator.token

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
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
        redisTemplate.opsForValue().increment(key, delta)
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
}

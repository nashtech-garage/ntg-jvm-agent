package com.ntgjvmagent.orchestrator.token

import java.time.LocalDate
import java.util.UUID

interface DailyTokenUsageCache {
    fun getUsedTokens(
        userId: UUID,
        date: LocalDate,
    ): Long?

    fun incrementUsedTokens(
        userId: UUID,
        date: LocalDate,
        delta: Long,
    )

    fun setUsedTokens(
        userId: UUID,
        date: LocalDate,
        value: Long,
        ttlSeconds: Long,
    )
}

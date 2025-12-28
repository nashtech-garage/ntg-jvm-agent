package com.ntgjvmagent.orchestrator.usage.aggregate.projection

import java.time.LocalDate

interface UsageDailyTimeSeriesProjection {
    val usageDate: LocalDate
    val totalTokens: Long
    val promptTokens: Long
    val completionTokens: Long
}

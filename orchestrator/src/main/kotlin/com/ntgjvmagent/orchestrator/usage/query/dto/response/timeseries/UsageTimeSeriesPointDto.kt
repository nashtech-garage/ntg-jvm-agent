package com.ntgjvmagent.orchestrator.usage.query.dto.response.timeseries

import java.time.LocalDate

data class UsageTimeSeriesPointDto(
    val date: LocalDate,
    val totalTokens: Long,
    val promptTokens: Long,
    val completionTokens: Long,
)

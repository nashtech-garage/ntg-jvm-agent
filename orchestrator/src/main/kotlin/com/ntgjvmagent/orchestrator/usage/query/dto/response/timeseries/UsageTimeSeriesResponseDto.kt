package com.ntgjvmagent.orchestrator.usage.query.dto.response.timeseries

import com.ntgjvmagent.orchestrator.model.UsageGroupBy

data class UsageTimeSeriesResponseDto(
    val groupBy: UsageGroupBy,
    val points: List<UsageTimeSeriesPointDto>,
)

package com.ntgjvmagent.orchestrator.usage.query.dto.response.freshness

import java.time.LocalDate

data class UsageFreshnessDto(
    val latestAggregatedDate: LocalDate,
)

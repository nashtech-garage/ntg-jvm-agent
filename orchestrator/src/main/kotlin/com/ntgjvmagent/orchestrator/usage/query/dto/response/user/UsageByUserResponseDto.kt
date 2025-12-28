package com.ntgjvmagent.orchestrator.usage.query.dto.response.user

import java.time.LocalDate

data class UsageByUserResponseDto(
    val from: LocalDate,
    val to: LocalDate,
    val rows: List<UsageByUserRowDto>,
)

package com.ntgjvmagent.orchestrator.usage.query.dto.response.agent

import java.time.LocalDate

data class UsageByAgentResponseDto(
    val from: LocalDate,
    val to: LocalDate,
    val rows: List<UsageByAgentRowDto>,
)

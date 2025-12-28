package com.ntgjvmagent.orchestrator.usage.query.dto.response.user

import java.util.UUID

data class UsageByUserRowDto(
    val userId: UUID?,
    val userName: String?,
    val promptTokens: Long,
    val completionTokens: Long,
    val totalTokens: Long,
)

package com.ntgjvmagent.orchestrator.usage.query.dto.response.summary

data class UsageSummaryResponseDto(
    val totalTokens: Long,
    val promptTokens: Long,
    val completionTokens: Long,
)

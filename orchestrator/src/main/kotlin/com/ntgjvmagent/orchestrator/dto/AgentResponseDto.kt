package com.ntgjvmagent.orchestrator.dto

import java.time.LocalDateTime
import java.util.UUID

data class AgentResponseDto(
    val id: UUID,
    val name: String,
    val model: String,
    val description: String?,
    val temperature: Double,
    val maxTokens: Int,
    val topP: Double,
    val frequencyPenalty: Double,
    val presencePenalty: Double,
    val active: Boolean,
    val provider: String?,
    val settings: Map<String, Any>?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
)

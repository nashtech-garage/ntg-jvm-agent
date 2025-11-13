package com.ntgjvmagent.orchestrator.dto

import java.util.UUID

data class AgentToolResponseDto(
    val id: UUID,
    val name: String,
    val type: String?,
    val description: String?,
    val config: Map<String, Any>?,
    val active: Boolean,
)

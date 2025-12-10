package com.ntgjvmagent.orchestrator.dto.response

import java.util.UUID

data class AgentToolResponseDto(
    val toolId: UUID,
    val toolName: String,
    val toolType: String?,
    val availableTo: String,
    val lastModifiedBy: String?,
    val lastModifiedWhen: String?,
    val enabled: Boolean,
)

package com.ntgjvmagent.orchestrator.dto.response

import java.util.UUID

data class ToolWithAssignmentResponseDto(
    val toolId: UUID,
    val toolName: String,
    val toolDescription: String,
    val isAssigned: Boolean,
)

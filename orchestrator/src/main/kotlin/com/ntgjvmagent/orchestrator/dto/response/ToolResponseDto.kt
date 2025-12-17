package com.ntgjvmagent.orchestrator.dto.response

import java.util.UUID

data class ToolResponseDto(
    val id: UUID,
    val name: String,
    val type: String?,
    val description: String?,
    val definition: Map<String, Any>?,
    val active: Boolean,
)

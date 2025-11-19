package com.ntgjvmagent.orchestrator.dto

import jakarta.validation.constraints.NotBlank

data class ToolRequestDto(
    @field:NotBlank val name: String,
    val type: String? = null,
    val description: String? = null,
    val config: Map<String, Any>? = null,
    val active: Boolean = true,
)

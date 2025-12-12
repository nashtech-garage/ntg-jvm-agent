package com.ntgjvmagent.orchestrator.dto.internal

import jakarta.validation.constraints.NotBlank

data class ToolDataDto(
    @field:NotBlank val name: String,
    val type: String? = null,
    val baseUrl: String? = null,
    val description: String? = null,
    val definition: Map<String, Any>? = null,
    val config: Map<String, Any>? = null,
    val active: Boolean = true,
)

package com.ntgjvmagent.orchestrator.dto.request

import jakarta.validation.constraints.NotBlank

data class ToolRequestDto(
    @field:NotBlank
    val baseUrl: String,
    @field:NotBlank
    val transportType: String,
    @field:NotBlank
    val endpoint: String,
    val authorization: AuthenticationRequestDto,
)

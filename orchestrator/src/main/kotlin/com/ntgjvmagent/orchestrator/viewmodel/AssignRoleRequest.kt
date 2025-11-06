package com.ntgjvmagent.orchestrator.viewmodel

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class AssignRoleRequest(
    @field:NotBlank val username: String,
    @field:NotEmpty
    val roleNames: List<@NotBlank String>,
)

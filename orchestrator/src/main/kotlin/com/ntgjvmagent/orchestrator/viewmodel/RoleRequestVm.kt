package com.ntgjvmagent.orchestrator.viewmodel

import jakarta.validation.constraints.NotBlank

data class RoleRequestVm(
    @field:NotBlank val name: String,
    val description: String?,
)

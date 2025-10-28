package com.ntgjvmagent.orchestrator.viewmodel

data class RoleRequestVm(
    @field:NotBlank val name: String,
    val description: String?,
)

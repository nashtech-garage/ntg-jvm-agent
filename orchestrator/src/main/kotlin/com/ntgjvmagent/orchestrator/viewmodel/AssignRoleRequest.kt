package com.ntgjvmagent.orchestrator.viewmodel

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AssignRoleRequest(
    @field:NotNull val id: UUID,
    @field:NotEmpty
    val roleNames: List<@NotBlank String>,
)

package com.ntgjvmagent.orchestrator.viewmodel

data class AssignRoleRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val rolename: String,
)

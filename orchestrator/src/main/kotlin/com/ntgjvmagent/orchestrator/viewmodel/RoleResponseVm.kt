package com.ntgjvmagent.orchestrator.viewmodel

import java.util.UUID

data class RoleResponseVm(
    val id: UUID?,
    val name: String,
    val description: String?,
)

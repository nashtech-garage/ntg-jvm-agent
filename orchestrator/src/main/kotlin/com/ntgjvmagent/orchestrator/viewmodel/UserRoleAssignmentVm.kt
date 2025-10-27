package com.ntgjvmagent.orchestrator.viewmodel

import java.util.UUID

data class UserRoleAssignmentVm(
    val userId: UUID,
    val roleIds: List<UUID>,
)

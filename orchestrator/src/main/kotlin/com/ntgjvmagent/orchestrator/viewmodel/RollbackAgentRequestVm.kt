package com.ntgjvmagent.orchestrator.viewmodel

/**
 * Request body for API to rollback Agent to a specific revision.
 */
data class RollbackAgentRequestVm(
    val revision: Int,
    val includeSecrets: Boolean = false,
)

package com.ntgjvmagent.orchestrator.exception

import java.time.Instant

data class ApiErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val path: String? = null,
    val details: Any? = null,
)

package com.ntgjvmagent.orchestrator.viewmodel

import java.time.Instant

data class ErrorResponseVm(
    val status: Int,
    val error: String,
    val message: String?,
    val timestamp: Instant = Instant.now(),
)

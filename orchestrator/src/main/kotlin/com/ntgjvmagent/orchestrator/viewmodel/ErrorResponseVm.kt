package com.ntgjvmagent.orchestrator.viewmodel

import java.time.OffsetDateTime

data class ErrorResponseVm(
    val status: Int,
    val error: String,
    val message: String?,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
)

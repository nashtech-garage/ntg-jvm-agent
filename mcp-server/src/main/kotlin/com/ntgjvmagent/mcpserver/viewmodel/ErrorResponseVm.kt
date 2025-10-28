package com.ntgjvmagent.mcpserver.viewmodel

import java.time.ZonedDateTime

data class ErrorResponseVm(
    val status: Int,
    val error: String,
    val message: String?,
    val timestamp: ZonedDateTime = ZonedDateTime.now(),
)

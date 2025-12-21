package com.ntgjvmagent.orchestrator.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class TokenQuotaUnavailableException(
    message: String = "Token quota service is temporarily unavailable",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

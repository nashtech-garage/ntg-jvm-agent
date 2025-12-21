package com.ntgjvmagent.orchestrator.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthenticatedUserException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

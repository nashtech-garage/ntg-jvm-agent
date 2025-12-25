package com.ntgjvmagent.orchestrator.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
class TokenLimitExceededException(
    message: String,
) : RuntimeException(message)

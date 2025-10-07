package com.ntgjvmagent.orchestrator.exception

class BadRequestException(
    message: String = "Bad request",
) : RuntimeException(message)

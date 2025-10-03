package com.ntgjvmagent.orchestrator.exception

class ResourceNotFoundException(
    message: String = "Resource not found",
) : RuntimeException(message)

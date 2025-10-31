package com.ntgjvmagent.mcpserver.exception

class ResourceNotFoundException(
    message: String = "Resource not found",
) : RuntimeException(message)

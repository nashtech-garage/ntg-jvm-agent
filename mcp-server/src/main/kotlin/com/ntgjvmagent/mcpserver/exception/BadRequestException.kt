package com.ntgjvmagent.mcpserver.exception

class BadRequestException(
    message: String = "Bad request",
) : RuntimeException(message)

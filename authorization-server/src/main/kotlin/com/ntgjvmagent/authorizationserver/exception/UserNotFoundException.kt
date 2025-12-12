package com.ntgjvmagent.authorizationserver.exception

class UserNotFoundException(
    message: String = "User not found"
) : RuntimeException(message)

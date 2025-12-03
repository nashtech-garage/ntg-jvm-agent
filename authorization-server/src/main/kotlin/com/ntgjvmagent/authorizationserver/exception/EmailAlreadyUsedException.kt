package com.ntgjvmagent.authorizationserver.exception

class EmailAlreadyUsedException(
    message: String = "Email is already in use"
) : RuntimeException(message)

package com.ntgjvmagent.authorizationserver.exception

class UsernameAlreadyUsedException(
    message: String = "Username is already in use"
) : RuntimeException(message)

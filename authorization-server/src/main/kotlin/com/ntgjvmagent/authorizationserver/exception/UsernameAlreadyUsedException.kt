package com.ntgjvmagent.authorizationserver.exception

class UsernameAlreadyUsedException(
    message: String = "Username is already used"
) : RuntimeException(message)

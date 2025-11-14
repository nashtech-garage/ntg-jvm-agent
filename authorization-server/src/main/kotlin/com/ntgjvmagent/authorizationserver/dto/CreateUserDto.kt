package com.ntgjvmagent.authorizationserver.dto

data class CreateUserDto(
    val username: String,
    val name: String,
    val email: String,
    val roles: Set<String>,
    val temporaryPassword: String
)


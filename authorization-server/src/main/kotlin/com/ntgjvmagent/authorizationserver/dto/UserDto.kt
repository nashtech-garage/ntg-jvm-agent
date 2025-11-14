package com.ntgjvmagent.authorizationserver.dto

data class UserDto(
    val username: String,
    val enabled: Boolean,
    val name: String,
    val email: String,
    val roles: Set<String>
)

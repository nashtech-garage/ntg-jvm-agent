package com.ntgjvmagent.authorizationserver.dto

import java.util.UUID

data class UserDto(
    val id: UUID?,
    val username: String,
    val enabled: Boolean,
    val name: String,
    val email: String,
    val roles: Set<String>
)

package com.ntgjvmagent.authorizationserver.dto

import java.util.UUID

data class UpdateUserResponseDto(
    val id: UUID?,
    val username: String,
    val name: String,
    val email: String,
    val enabled: Boolean,
    val roles: Set<String>
)

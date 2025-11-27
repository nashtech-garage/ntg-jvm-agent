package com.ntgjvmagent.authorizationserver.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateUserRequestDto(
    @field:NotBlank(message = "Username cannot be blank")
    @field:Size(max = 255, message = "Username too long")
    val username: String? = null,

    @field:NotBlank(message = "Name cannot be blank")
    @field:Size(max = 255, message = "Name too long")
    val name: String? = null,

    @field:NotBlank(message = "Email cannot be blank")
    @field:Email(message = "Email format is invalid")
    @field:Size(max = 255, message = "Email too long")
    val email: String? = null,
)

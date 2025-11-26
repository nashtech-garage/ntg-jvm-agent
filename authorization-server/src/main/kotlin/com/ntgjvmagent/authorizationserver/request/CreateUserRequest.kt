package com.ntgjvmagent.authorizationserver.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import com.ntgjvmagent.authorizationserver.enum.UserRoleEnum
import com.ntgjvmagent.authorizationserver.constants.UserCreationConstants

data class CreateUserRequest(
    @field:NotBlank(message = "Username must not be blank")
    @field:Size(min = 5, max = 30, message = "Username must be between 5 and 30 characters")
    val username: String,

    @field:NotBlank(message = "Name must not be blank")
    @field:Size(min = 1, max = 30, message = "Name must be between 1 and 30 characters")
    val name: String,

    @field:NotBlank(message = "Email must not be blank")
    @field:Email(message = "Invalid email format")
    val email: String,

    val sendAccountInfo: Boolean,

    val roles: Set<String> = setOf(UserRoleEnum.ROLE_USER.roleName)
) {
    init {
        // Validate that role is allowed
        require(roles.all { it in UserCreationConstants.ALLOWED_USER_CREATION_ROLES }) {
            "Only ${UserCreationConstants.ALLOWED_USER_CREATION_ROLES.joinToString()} " +
                "role are allowed when creating new users. Received: ${roles.joinToString()}"
        }
    }
}

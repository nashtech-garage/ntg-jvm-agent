package com.ntgjvmagent.authorizationserver.constants

import com.ntgjvmagent.authorizationserver.enum.UserRoleEnum

object UserCreationConstants {
    val ALLOWED_USER_CREATION_ROLES = setOf(UserRoleEnum.ROLE_USER.roleName)
}


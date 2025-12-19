package com.ntgjvmagent.orchestrator.component

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CurrentUserProvider {
    fun getUserId(): UUID =
        (SecurityContextHolder.getContext()?.authentication?.principal as? Jwt)
            ?.getClaim<String>("user_id")
            ?.let { UUID.fromString(it) }
            ?: error("No authenticated user found")
}

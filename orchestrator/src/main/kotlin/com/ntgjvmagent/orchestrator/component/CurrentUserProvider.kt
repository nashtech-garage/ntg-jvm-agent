package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.exception.UnauthenticatedUserException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CurrentUserProvider {
    fun getUserId(): UUID {
        val context =
            SecurityContextHolder.getContext()
                ?: fail("SecurityContext is null (no authentication present)")

        val authentication =
            context.authentication
                ?: fail("Authentication is null in SecurityContext")

        val jwt =
            authentication.principal as? Jwt
                ?: fail(
                    "Unsupported principal type: ${authentication.principal?.javaClass?.name}",
                )

        val userIdClaim =
            jwt.getClaim<String>("user_id")
                ?: fail("JWT does not contain required claim 'user_id'")

        return runCatching { UUID.fromString(userIdClaim) }
            .getOrElse {
                fail("Invalid UUID format for user_id claim: '$userIdClaim'", it)
            }
    }

    fun getUserIdOrNull(): UUID? = runCatching { getUserId() }.getOrNull()

    private fun fail(
        message: String,
        cause: Throwable? = null,
    ): Nothing = throw UnauthenticatedUserException(message, cause)
}

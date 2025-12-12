package com.ntgjvmagent.orchestrator.entity.base.auditoraware

import com.ntgjvmagent.orchestrator.entity.User
import com.ntgjvmagent.orchestrator.repository.UserRepository
import org.springframework.data.domain.AuditorAware
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.UUID

@Component("auditorAware")
class AuditorAwareImpl(
    private val userRepository: UserRepository,
) : AuditorAware<User> {
    override fun getCurrentAuditor(): Optional<User> =
        (SecurityContextHolder.getContext()?.authentication?.principal as? Jwt)
            ?.getClaim<String>("user_id") // extract userId from JWT claim
            ?.let { UUID.fromString(it) } // convert to UUID
            ?.let { userRepository.findByIdOrNull(it) } // load User entity
            ?.let { Optional.of(it) } ?: Optional.empty()
}

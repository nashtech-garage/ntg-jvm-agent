package com.ntgjvmagent.orchestrator.entity.audit

import org.hibernate.envers.RevisionListener
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

/**
 * Revision Listener for Hibernate Envers.
 * Automatically populates revision entity with current user information from security context.
 */
class EnversRevisionListener : RevisionListener {

    override fun newRevision(revisionEntity: Any) {
        if (revisionEntity is RevisionEntity) {
            val authentication: Authentication? = SecurityContextHolder.getContext().authentication

            if (authentication != null && authentication.isAuthenticated) {
                // Extract username
                revisionEntity.username = authentication.name

                // Try to extract user ID from JWT claims if available
                if (authentication.principal is Jwt) {
                    val jwt = authentication.principal as Jwt
                    val userIdClaim = jwt.getClaim<String>("sub")
                    if (!userIdClaim.isNullOrBlank()) {
                        revisionEntity.userId = userIdClaim.toUuidOrNull()
                    }
                }
            } else {
                // For system operations or when user is not authenticated
                revisionEntity.username = "system"
            }
        }
    }
}

private fun String.toUuidOrNull(): UUID? =
    try {
        UUID.fromString(this)
    } catch (_: IllegalArgumentException) {
        null
    }

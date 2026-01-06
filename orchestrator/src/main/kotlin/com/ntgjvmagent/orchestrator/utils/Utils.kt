package com.ntgjvmagent.orchestrator.utils

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID

object Utils {
    fun getShortToolName(originalToolName: String?): String? =
        originalToolName
            ?.takeIf { it.isNotBlank() }
            ?.substringAfterLast("_")
            ?.takeIf { it.isNotBlank() }

    fun currentUserId(authentication: Authentication): UUID = UUID.fromString(authentication.name)

    fun currentUserName(auth: Authentication): String = (auth as JwtAuthenticationToken).token.getClaimAsString("name")
}

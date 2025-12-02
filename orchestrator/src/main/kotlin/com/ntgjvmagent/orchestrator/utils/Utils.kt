package com.ntgjvmagent.orchestrator.utils

import org.springframework.security.core.Authentication
import java.util.UUID

object Utils {
    fun getShortToolName(originalToolName: String?): String? =
        originalToolName
            ?.takeIf { it.isNotBlank() }
            ?.substringAfterLast("_")
            ?.takeIf { it.isNotBlank() }

    fun currentUserId(authentication: Authentication): UUID = UUID.fromString(authentication.name)
}

package com.ntgjvmagent.orchestrator.utils

object Utils {
    fun getShortToolName(originalToolName: String?): String? =
        originalToolName
            ?.takeIf { it.isNotBlank() }
            ?.substringAfterLast("_")
            ?.takeIf { it.isNotBlank() }
}

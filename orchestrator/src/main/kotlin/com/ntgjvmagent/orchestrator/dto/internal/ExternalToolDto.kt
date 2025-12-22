package com.ntgjvmagent.orchestrator.dto.internal

interface ExternalToolDto {
    fun getBaseUrl(): String

    fun getConfig(): Map<String, Any>
}

package com.ntgjvmagent.orchestrator.dto

interface ExternalToolDto {
    fun getBaseUrl(): String

    fun getConfig(): Map<String, Any>
}

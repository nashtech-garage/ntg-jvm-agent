package com.ntgjvmagent.orchestrator.chat.handlers

internal object OpenAiEndpoint {
    fun serviceBaseUrl(
        baseUrl: String,
        endpoint: String,
        standardSuffix: String,
    ): String {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val normalizedEndpoint = endpoint.trim().let { if (it.startsWith('/')) it else "/$it" }
        val prefix = normalizedEndpoint.removeSuffix(standardSuffix).trimEnd('/')
        return normalizedBaseUrl + prefix
    }
}

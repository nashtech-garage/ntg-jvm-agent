package com.ntgjvmagent.orchestrator.chat.handlers

internal object OpenAiEndpoint {
    fun serviceBaseUrl(
        baseUrl: String,
        endpoint: String,
        standardSuffix: String,
    ): String {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val normalizedEndpoint = endpoint.trim().trimEnd('/').let { if (it.startsWith('/')) it else "/$it" }
        val prefix = normalizedEndpoint.removeSuffix(standardSuffix).trimEnd('/')
        return if (prefix.isEmpty() ||
            normalizedBaseUrl.endsWith(prefix)
        ) {
            normalizedBaseUrl
        } else {
            normalizedBaseUrl + prefix
        }
    }
}

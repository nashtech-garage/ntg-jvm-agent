package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val DEFAULT_INITIALIZATION_TIMEOUT_SECONDS = 10L
private const val DEFAULT_REQUEST_TIMEOUT_SECONDS = 30L

@ConfigurationProperties(prefix = "mcp.dynamic-client")
data class McpDynamicClientProperties(
    val initializationTimeout: Duration = Duration.ofSeconds(DEFAULT_INITIALIZATION_TIMEOUT_SECONDS),
    val requestTimeout: Duration = Duration.ofSeconds(DEFAULT_REQUEST_TIMEOUT_SECONDS),
)

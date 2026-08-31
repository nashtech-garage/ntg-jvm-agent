package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "chat.reasoning")
data class ChatReasoningProperties(
    val enabled: Boolean = false,
)

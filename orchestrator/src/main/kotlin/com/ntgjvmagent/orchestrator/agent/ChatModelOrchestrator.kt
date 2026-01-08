package com.ntgjvmagent.orchestrator.agent

import com.ntgjvmagent.orchestrator.agent.handlers.ChatModelHandler
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Orchestrator for ChatModel creation based on provider type.
 * Delegates to specific handler implementations for each provider.
 */
@Service
class ChatModelOrchestrator(
    private val handlers: List<ChatModelHandler>,
) {
    fun createChatModel(config: ChatModelConfig): ChatModel {
        val handler =
            handlers.find { it.supports(config.providerType) }
                ?: throw IllegalArgumentException(
                    "No handler found for provider type: ${config.providerType}",
                )

        return handler.createChatModel(config)
    }
}

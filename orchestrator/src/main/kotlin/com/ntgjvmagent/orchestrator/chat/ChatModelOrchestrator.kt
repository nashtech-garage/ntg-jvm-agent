package com.ntgjvmagent.orchestrator.chat

import com.ntgjvmagent.orchestrator.chat.handlers.ChatModelHandler
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
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
        val handler = resolveHandler(config.providerType)
        return handler.createChatModel(config)
    }

    private fun resolveHandler(providerType: ProviderType): ChatModelHandler =
        handlers.firstOrNull { it.supports(providerType) }
            ?: throw IllegalArgumentException(
                "No ChatModelHandler registered for provider: $providerType",
            )
}

package com.ntgjvmagent.orchestrator.agent.handlers

import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Anthropic ChatModel Handler - Not yet implemented
 */
@Service
class AnthropicChatModelHandler : ChatModelHandler {
    override fun supports(providerType: ProviderType) = providerType == ProviderType.ANTHROPIC

    override fun createChatModel(config: ChatModelConfig): ChatModel =
        throw UnsupportedOperationException(
            "Anthropic is not yet supported. Support will be added in future versions.",
        )
}

package com.ntgjvmagent.orchestrator.agent.handlers

import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Ollama ChatModel Handler - Not yet implemented
 */
@Service
class OllamaChatModelHandler : ChatModelHandler {
    override fun supports(providerType: ProviderType) = providerType == ProviderType.OLLAMA

    override fun createChatModel(config: ChatModelConfig): ChatModel =
        throw UnsupportedOperationException(
            "Ollama is not yet supported. Support will be added in future versions.",
        )
}

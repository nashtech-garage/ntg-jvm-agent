package com.ntgjvmagent.orchestrator.chat.handlers

import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Bedrock ChatModel Handler - Not yet implemented
 */
@Service
class BedrockChatModelHandler : ChatModelHandler {
    override fun supports(providerType: ProviderType) = providerType == ProviderType.BEDROCK

    override fun createChatModel(config: ChatModelConfig): ChatModel =
        throw UnsupportedOperationException(
            "Bedrock is not yet supported. Support will be added in future versions.",
        )
}

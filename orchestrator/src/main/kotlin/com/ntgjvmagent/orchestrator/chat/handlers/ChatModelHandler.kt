package com.ntgjvmagent.orchestrator.chat.handlers

import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import org.springframework.ai.chat.model.ChatModel

interface ChatModelHandler {
    fun supports(providerType: ProviderType): Boolean

    fun createChatModel(config: ChatModelConfig): ChatModel
}

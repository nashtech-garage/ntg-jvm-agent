package com.ntgjvmagent.orchestrator.agent.handlers

import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import org.springframework.ai.chat.model.ChatModel

interface ChatModelHandler {
    fun supports(providerType: ProviderType): Boolean

    fun createChatModel(config: ChatModelConfig): ChatModel
}

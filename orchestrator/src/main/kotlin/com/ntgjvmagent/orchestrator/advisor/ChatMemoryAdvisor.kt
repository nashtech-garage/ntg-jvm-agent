package com.ntgjvmagent.orchestrator.advisor

import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatMemoryAdvisor {
    @Bean
    fun customChatMemory(chatMessageRepository: ChatMessageRepository): CustomChatMemory =
        CustomChatMemory(chatMessageRepository)

    @Bean
    fun messageChatMemoryAdvisor(chatMemory: CustomChatMemory): MessageChatMemoryAdvisor =
        MessageChatMemoryAdvisor
            .builder(chatMemory)
            .order(Constant.MESSAGE_HISTORY_ADVISOR_ORDER)
            .build()
}

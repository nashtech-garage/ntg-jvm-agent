package com.ntgjvmagent.orchestrator.advisor

import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.data.domain.PageRequest
import java.util.UUID

class CustomChatMemory(
    private val chatMessageRepository: ChatMessageRepository,
) : ChatMemory {
    override fun get(conversationId: String): List<Message?> {
        val conversationId = UUID.fromString(conversationId)

        val recentMessages =
            chatMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, Constant.HISTORY_LIMIT))
                .asReversed()

        return recentMessages.mapNotNull { it.toChatMemoryMessage() }
    }

    // no implementation since we already do it manually
    // also implement this would not work with LLM stream calls due to its flow in Spring AI
    override fun add(
        conversationId: String,
        messages: List<Message>,
    ) {
        TODO("Not yet implemented")
    }

    override fun clear(conversationId: String) {
        TODO("Not yet implemented")
    }

    fun ChatMessageEntity.toChatMemoryMessage(): Message? =
        when (this.type) {
            Constant.QUESTION_TYPE -> UserMessage(this.content)
            Constant.ANSWER_TYPE -> AssistantMessage(this.content)
            else -> null
        }
}

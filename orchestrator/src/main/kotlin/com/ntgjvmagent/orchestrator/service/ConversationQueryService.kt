package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.mapper.ChatMessageMapper
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ConversationQueryService(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
) {
    fun listConversationByUser(userId: UUID): List<ConversationResponseVm> =
        conversationRepo.listActiveConversationsByUser(userId)

    fun listMessageByConversation(conversationId: UUID): List<ChatMessageResponseVm> {
        conversationRepo
            .findById(conversationId)
            .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }

        return messageRepo
            .listMessageByConversationIdOrdered(conversationId)
            .map(ChatMessageMapper::toResponse)
    }
}

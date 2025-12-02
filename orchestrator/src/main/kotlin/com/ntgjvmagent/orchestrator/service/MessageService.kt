package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.enums.MessageReaction
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.mapper.ChatMessageMapper
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MessageService(
    private val messageRepo: ChatMessageRepository,
) {
    @Transactional
    fun reactMessage(
        messageId: UUID,
        reaction: MessageReaction,
        username: String,
    ): ChatMessageResponseVm {
        val message =
            this.messageRepo
                .findById(messageId)
                .orElseThrow { ResourceNotFoundException("Message not found: $messageId") }

        if (message.conversation.username != username || message.type != Constant.ANSWER_TYPE) {
            throw BadRequestException("You are not allowed to react to this message")
        }

        message.reaction = reaction
        this.messageRepo.save(message)
        return ChatMessageMapper.toResponse(message)
    }
}

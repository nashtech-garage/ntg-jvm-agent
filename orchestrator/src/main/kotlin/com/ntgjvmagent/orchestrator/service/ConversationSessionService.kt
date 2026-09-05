package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.model.ChatMessageType
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.session.CreateSessionRequest
import org.springframework.ai.session.SessionEvent
import org.springframework.ai.session.SessionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ConversationSessionService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: ChatMessageRepository,
    private val sessionService: SessionService,
) {
    @Transactional
    fun resolveSessionId(
        conversationId: UUID?,
        userId: UUID,
        agentId: UUID,
        correlationId: String,
    ): UUID =
        if (conversationId == null) {
            createSession(userId, agentId, correlationId)
        } else {
            resolveExistingSession(conversationId, userId, agentId, correlationId)
        }

    private fun resolveExistingSession(
        conversationId: UUID,
        userId: UUID,
        agentId: UUID,
        correlationId: String,
    ): UUID {
        val conversation =
            conversationRepository.findByIdForSessionUpdate(conversationId)
                ?: throw ResourceNotFoundException("Conversation not found: $conversationId")

        // Ownership is checked before anything is returned: a conversation that already has a
        // session used to short-circuit past this check and hand its id to any caller. Nothing
        // leaked, because ChatModelService and SessionMemoryAdvisor both reject a user/session
        // mismatch later on, but the guarantee belonged in another class and only held by
        // accident. 404 rather than 403 keeps the existence of someone else's conversation
        // private, matching ConversationCommandService.
        if (conversation.createdBy?.id != userId) {
            throw ResourceNotFoundException("Conversation not found: $conversationId")
        }

        conversation.sessionId?.let { return it }

        val sessionId = createSession(userId, agentId, correlationId)
        backfill(conversationId, sessionId)

        conversation.sessionId = sessionId
        conversationRepository.save(conversation)
        return sessionId
    }

    private fun createSession(
        userId: UUID,
        agentId: UUID,
        correlationId: String,
    ): UUID {
        val sessionId = UUID.randomUUID()
        sessionService.create(
            CreateSessionRequest
                .builder()
                .id(sessionId.toString())
                .userId(userId.toString())
                .metadata("agentId", agentId.toString())
                .metadata("correlationId", correlationId)
                .build(),
        )
        return sessionId
    }

    private fun backfill(
        conversationId: UUID,
        sessionId: UUID,
    ) {
        messageRepository
            .listMessageByConversationIdOrdered(conversationId)
            .forEach { message ->
                val sessionMessage =
                    when (message.type) {
                        ChatMessageType.QUESTION -> UserMessage(message.content)
                        ChatMessageType.ANSWER -> AssistantMessage(message.content)
                    }
                sessionService.appendEvent(
                    SessionEvent
                        .builder()
                        .id("backfill:${message.id}")
                        .sessionId(sessionId.toString())
                        .timestamp(requireNotNull(message.createdAt))
                        .message(sessionMessage)
                        .build(),
                )
            }
    }
}

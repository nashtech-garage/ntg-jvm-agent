package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.dto.ChatResponseDto
import com.ntgjvmagent.orchestrator.entity.ChatMessage
import com.ntgjvmagent.orchestrator.entity.ChatMessageMedia
import com.ntgjvmagent.orchestrator.entity.Conversation
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.model.ChatMessageType
import com.ntgjvmagent.orchestrator.model.MessageReaction
import com.ntgjvmagent.orchestrator.repository.ChatMessageMediaRepository
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.repository.UserRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVmImpl
import jakarta.persistence.EntityNotFoundException
import org.springframework.ai.session.SessionService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ConversationCommandService(
    private val chatModelService: ChatModelService,
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
    private val messageMediaRepo: ChatMessageMediaRepository,
    private val userRepository: UserRepository,
    private val sessionService: SessionService,
) {
    // --------------------------------------------------
    // Delete / Update
    // --------------------------------------------------

    @Transactional
    fun deleteConversation(conversationId: UUID) {
        val conversation = findConversation(conversationId)
        conversation.isActive = false

        // Nothing ever flips isActive back on, so the conversation -- and with it its memory --
        // is unreachable from here. Drop the session (its events cascade) instead of leaving it
        // to accumulate, and clear the pointer so a stale id can never be handed out again.
        conversation.sessionId?.let { sessionId ->
            conversation.sessionId = null
            sessionService.delete(sessionId.toString())
        }

        conversationRepo.save(conversation)
    }

    @Transactional
    fun updateConversationTitle(
        conversationId: UUID,
        newTitle: String,
        userId: UUID,
    ): ConversationResponseVm {
        val conversation = findConversation(conversationId)

        if (conversation.createdBy?.id != userId) {
            throw ResourceNotFoundException("Conversation not found: $conversationId")
        }

        conversation.title = newTitle.trim()
        val saved = conversationRepo.save(conversation)

        return ConversationResponseVmImpl(
            saved.id ?: conversationId,
            saved.title,
            saved.createdAt!!,
        )
    }

    // --------------------------------------------------
    // Create (first message only)
    // --------------------------------------------------

    @Transactional
    fun createConversationWithFirstMessage(
        userId: UUID,
        chatReq: ChatRequestDto,
        answer: String,
        sessionId: UUID,
    ): ChatResponseDto {
        val createdBy =
            userRepository.findByIdOrNull(userId)
                ?: throw EntityNotFoundException("User $userId not found")

        // 1) Create conversation (exists == successful stream)
        val conversation =
            conversationRepo.save(
                Conversation(
                    title = "",
                    sessionId = sessionId,
                ).also {
                    it.createdBy = createdBy
                },
            )

        // 2) Save question
        val question =
            messageRepo.save(
                ChatMessage(
                    content = chatReq.question,
                    conversation = conversation,
                    type = ChatMessageType.QUESTION,
                ).also {
                    it.createdBy = createdBy
                },
            )

        saveMessageMedia(chatReq, question)

        // 3) Save answer
        val answerEntity =
            messageRepo.save(
                ChatMessage(
                    content = answer,
                    conversation = conversation,
                    type = ChatMessageType.ANSWER,
                ).also {
                    it.createdBy = createdBy
                },
            )

        // 4) Generate title ONCE (first message only)
        val title =
            chatModelService.createSummarize(
                userId = userId,
                agentId = chatReq.agentId,
                correlationId = chatReq.correlationId,
                question = chatReq.question,
            ) ?: chatReq.question

        conversation.title = title
        conversationRepo.save(conversation)

        return buildResponse(conversation, answerEntity)
    }

    // --------------------------------------------------
    // Append (follow-up messages)
    // --------------------------------------------------

    @Transactional
    fun appendConversationMessage(
        userId: UUID,
        chatReq: ChatRequestDto,
        answer: String,
    ): ChatResponseDto =
        appendConversationMessage(
            userId = userId,
            chatReq = chatReq,
            questionContent = chatReq.question,
            answer = answer,
        )

    @Transactional
    fun appendConversationAnswerTurn(
        userId: UUID,
        chatReq: ChatRequestDto,
        questionContent: String,
        answer: String,
    ): ChatResponseDto =
        appendConversationMessage(
            userId = userId,
            chatReq = chatReq,
            questionContent = questionContent,
            answer = answer,
        )

    private fun appendConversationMessage(
        userId: UUID,
        chatReq: ChatRequestDto,
        questionContent: String,
        answer: String,
    ): ChatResponseDto {
        val conversationId =
            chatReq.conversationId
                ?: error("conversationId must be provided to append message")

        val conversation = findConversation(conversationId)

        // ownership check
        if (conversation.createdBy?.id != userId) {
            throw ResourceNotFoundException("Conversation not found: $conversationId")
        }

        // Save question
        val question =
            messageRepo.save(
                ChatMessage(
                    content = questionContent,
                    conversation = conversation,
                    type = ChatMessageType.QUESTION,
                ),
            )

        saveMessageMedia(chatReq, question)

        // Save answer
        val answerEntity =
            messageRepo.save(
                ChatMessage(
                    content = answer,
                    conversation = conversation,
                    type = ChatMessageType.ANSWER,
                ),
            )

        return buildResponse(conversation, answerEntity)
    }

    @Transactional
    fun persistPendingQuestionTurn(
        userId: UUID,
        chatReq: ChatRequestDto,
        sessionId: UUID,
    ): UUID {
        val createdBy =
            userRepository.findByIdOrNull(userId)
                ?: throw EntityNotFoundException("User $userId not found")
        val conversation =
            chatReq.conversationId?.let { conversationId ->
                findConversation(conversationId).also {
                    if (it.createdBy?.id != userId) {
                        throw ResourceNotFoundException("Conversation not found: $conversationId")
                    }
                    check(it.sessionId == sessionId) { "Pending question session mismatch" }
                }
            } ?: conversationRepo.save(
                Conversation(
                    title = chatReq.question,
                    sessionId = sessionId,
                ).also { it.createdBy = createdBy },
            )

        val question =
            messageRepo.save(
                ChatMessage(
                    content = chatReq.question,
                    conversation = conversation,
                    type = ChatMessageType.QUESTION,
                ).also { it.createdBy = createdBy },
            )
        saveMessageMedia(chatReq, question)

        return requireNotNull(conversation.id)
    }

    // --------------------------------------------------
    // Helpers
    // --------------------------------------------------

    private fun buildResponse(
        conversation: Conversation,
        answerEntity: ChatMessage,
    ): ChatResponseDto =
        ChatResponseDto(
            ConversationResponseVmImpl(
                conversation.id!!,
                conversation.title,
                conversation.createdAt!!,
            ),
            ChatMessageResponseVm(
                answerEntity.id!!,
                answerEntity.content,
                answerEntity.createdAt!!,
                ChatMessageType.ANSWER,
                emptyList(),
                MessageReaction.NONE,
            ),
        )

    private fun saveMessageMedia(
        chatReq: ChatRequestDto,
        questionEntity: ChatMessage,
    ) {
        val files = chatReq.files ?: return
        if (files.isEmpty()) return

        val media =
            files.map {
                ChatMessageMedia(
                    fileName = it.originalFilename ?: it.name,
                    contentType = it.contentType ?: Constant.PNG_CONTENT_TYPE,
                    chatMessage = questionEntity,
                    data = it.bytes,
                    fileSize = it.size,
                )
            }

        messageMediaRepo.saveAll(media)
    }

    private fun findConversation(id: UUID): Conversation =
        conversationRepo
            .findById(id)
            .orElseThrow {
                ResourceNotFoundException("Conversation not found: $id")
            }
}

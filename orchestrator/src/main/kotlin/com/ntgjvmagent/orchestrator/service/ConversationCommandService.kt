package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.dto.ChatResponseDto
import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.entity.ChatMessageMediaEntity
import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.model.ConversationStatus
import com.ntgjvmagent.orchestrator.model.MessageReaction
import com.ntgjvmagent.orchestrator.repository.ChatMessageMediaRepository
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVmImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ConversationCommandService(
    private val chatModelService: ChatModelService,
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
    private val messageMediaRepo: ChatMessageMediaRepository,
    private val chatMemoryService: ChatMemoryService,
) {
    @Transactional
    fun createEmptyConversation(): UUID {
        val conversation =
            ConversationEntity(
                title = "",
                status = ConversationStatus.DRAFT,
            )

        return conversationRepo
            .save(conversation)
            .id
            ?: error("Can't create conversation")
    }

    @Transactional
    fun deleteConversation(conversationId: UUID) {
        val conversation = findConversation(conversationId)
        conversation.isActive = false
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
            throw ResourceNotFoundException(
                "Conversation not found: $conversationId",
            )
        }

        conversation.title = newTitle.trim()
        val saved = conversationRepo.save(conversation)

        return ConversationResponseVmImpl(
            saved.id ?: conversationId,
            saved.title,
            saved.createdAt!!,
        )
    }

    @Transactional
    fun appendConversationMessage(
        userId: UUID,
        chatReq: ChatRequestDto,
        answer: String,
    ): ChatResponseDto {
        val conversationId =
            chatReq.conversationId
                ?: error("conversationId must exist")

        val conversation = findConversation(conversationId)

        val question =
            messageRepo.save(
                ChatMessageEntity(
                    content = chatReq.question,
                    conversation = conversation,
                    type = Constant.QUESTION_TYPE,
                ),
            )

        saveMessageMedia(chatReq, question)

        chatMemoryService.onMessageSaved(
            agentId = chatReq.agentId,
            conversationId = conversation.id!!,
            role = Constant.QUESTION_TYPE,
            content = chatReq.question,
        )

        val answerEntity =
            messageRepo.save(
                ChatMessageEntity(
                    content = answer,
                    conversation = conversation,
                    type = Constant.ANSWER_TYPE,
                ),
            )

        if (conversation.status == ConversationStatus.DRAFT) {
            val title =
                chatModelService
                    .createSummarize(
                        userId,
                        chatReq.agentId,
                        chatReq.correlationId,
                        chatReq.question,
                    )
                    ?: chatReq.question

            conversation.title = title
            conversation.status = ConversationStatus.ACTIVE
            conversationRepo.save(conversation)
        }

        return ChatResponseDto(
            ConversationResponseVmImpl(
                conversation.id!!,
                conversation.title,
                conversation.createdAt!!,
            ),
            ChatMessageResponseVm(
                answerEntity.id!!,
                answerEntity.content,
                answerEntity.createdAt!!,
                Constant.ANSWER_TYPE,
                emptyList(),
                MessageReaction.NONE,
            ),
        )
    }

    private fun saveMessageMedia(
        chatReq: ChatRequestDto,
        questionEntity: ChatMessageEntity,
    ) {
        val files = chatReq.files ?: return
        if (files.isEmpty()) return

        val media =
            files.map {
                ChatMessageMediaEntity(
                    fileName = it.originalFilename ?: it.name,
                    contentType =
                        it.contentType ?: Constant.PNG_CONTENT_TYPE,
                    chatMessage = questionEntity,
                    data = it.bytes,
                    fileSize = it.size,
                )
            }

        messageMediaRepo.saveAll(media)
    }

    private fun findConversation(id: UUID): ConversationEntity =
        conversationRepo
            .findById(id)
            .orElseThrow {
                ResourceNotFoundException(
                    "Conversation not found: $id",
                )
            }
}

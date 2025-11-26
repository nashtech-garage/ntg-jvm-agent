package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.entity.ChatMessageMediaEntity
import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.mapper.ChatMessageMapper
import com.ntgjvmagent.orchestrator.repository.ChatMessageMediaRepository
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatRequestVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVmImpl
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ConversationService(
    private val chatModelService: ChatModelService,
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
    private val messageMediaRepo: ChatMessageMediaRepository,
) {
    @Transactional
    fun createConversation(
        chatReq: ChatRequestVm,
        username: String,
    ): ChatResponseVm {
        if (chatReq.conversationId != null) {
            return this.addMessage(chatReq.conversationId, chatReq)
        }
        val conversationId = this.createNewConversation(chatReq, username)
        return this.addMessage(conversationId, chatReq)
    }

    fun listConversationByUser(username: String): List<ConversationResponseVm> {
        val conversations = this.conversationRepo.listActiveConversationsByUser(username)
        return conversations
    }

    fun listMessageByConversation(conversationId: UUID): List<ChatMessageResponseVm> {
        this.conversationRepo
            .findById(conversationId)
            .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }
        return this.messageRepo.listMessageByConversationId(conversationId).map { ChatMessageMapper.toResponse(it) }
    }

    private fun createNewConversation(
        chatReq: ChatRequestVm,
        username: String,
    ): UUID {
        val titleSummarize = this.chatModelService.createSummarize(chatReq.agentId, chatReq.question)
        val conversation =
            ConversationEntity(
                title = titleSummarize ?: chatReq.question,
                username = username,
            )
        val conversationId: UUID? = this.conversationRepo.save(conversation).id
        conversationId ?: throw BadRequestException("Can't create new conversation")
        return conversationId
    }

    private fun addMessage(
        conversationId: UUID,
        request: ChatRequestVm,
    ): ChatResponseVm {
        val conversation =
            this.conversationRepo
                .findById(conversationId)
                .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }
        // Save question into DB first
        val questionMsg =
            ChatMessageEntity(
                content = request.question,
                conversation = conversation,
                type = Constant.QUESTION_TYPE,
            )
        val questionEntity = this.messageRepo.save(questionMsg)
        saveMessageMedia(request, questionEntity)
        val conversationResponse: ConversationResponseVm =
            ConversationResponseVmImpl(
                conversationId,
                conversation.title,
                conversation.createdAt!!,
            )

        val history =
            messageRepo
                .listMessageByConversationId(conversationId)
                .map { ChatMessageMapper.toHistoryFormat(it) }

        val answer: String? =
            chatModelService.call(
                request = request,
                history = history,
            )

        // Only save reply if it has actual reply
        answer?.let {
            val answerMsg =
                ChatMessageEntity(
                    content = answer,
                    conversation = conversation,
                    type = Constant.ANSWER_TYPE,
                )
            val answerMsgEntity = this.messageRepo.save(answerMsg)
            return ChatResponseVm(
                conversationResponse,
                ChatMessageResponseVm(
                    answerMsgEntity.id ?: UUID.randomUUID(),
                    answerMsgEntity.content,
                    answerMsgEntity.createdAt!!,
                    type = Constant.ANSWER_TYPE,
                    medias = emptyList(),
                ),
            )
        }
        return ChatResponseVm(
            conversationResponse,
            null,
        )
    }

    @Transactional
    fun deleteConversation(conversationId: UUID) {
        val conversation =
            this.conversationRepo
                .findById(conversationId)
                .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }
        conversation.isActive = false
        this.conversationRepo.save(conversation)
    }

    @Transactional
    fun updateConversationTitle(
        conversationId: UUID,
        newTitle: String,
        username: String,
    ): ConversationResponseVm {
        val conversation =
            this.conversationRepo
                .findById(conversationId)
                .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }

        if (conversation.username != username) {
            // Do not reveal existence of the conversation to unauthorized users
            throw ResourceNotFoundException("Conversation not found: $conversationId")
        }

        conversation.title = newTitle.trim()
        val updatedConversation = this.conversationRepo.save(conversation)
        return ConversationResponseVmImpl(
            updatedConversation.id ?: conversationId,
            updatedConversation.title,
            updatedConversation.createdAt!!,
        )
    }

    private fun saveMessageMedia(
        chatReq: ChatRequestVm,
        questionEntity: ChatMessageEntity,
    ) {
        if (chatReq.files == null || chatReq.files.isEmpty()) {
            return
        }

        // Save media if it has
        val mediaEntities =
            chatReq.files.map {
                ChatMessageMediaEntity(
                    fileName = it.originalFilename ?: it.name,
                    contentType = it.contentType ?: Constant.PNG_CONTENT_TYPE,
                    chatMessage = questionEntity,
                    data = it.bytes,
                    fileSize = it.size,
                )
            }
        messageMediaRepo.saveAll(mediaEntities)
    }
}

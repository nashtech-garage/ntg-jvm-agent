package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.ChatResponseDto
import com.ntgjvmagent.orchestrator.dto.CitationDto
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ConversationService(
    private val chatModelService: ChatModelService,
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
    private val messageMediaRepo: ChatMessageMediaRepository,
    private val historyLimit: Int = 5,
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

    @Transactional(readOnly = true)
    fun listMessageByConversation(conversationId: UUID): List<ChatMessageResponseVm> {
        conversationRepo
            .findById(conversationId)
            .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }

        val messages = messageRepo.listMessageByConversationId(conversationId)

        return messages.map { ChatMessageMapper.toResponse(it) }
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

        val splitIndex = history.size - historyLimit

        val (olderMessages, recentMessages) =
            if (splitIndex > 0) {
                history.partition { history.indexOf(it) < splitIndex }
            } else {
                Pair(emptyList(), history)
            }

        val summary = chatModelService.createDynamicSummary(request.agentId, olderMessages)

        val answer =
            chatModelService.call(
                request = request,
                history = recentMessages,
                summary = summary,
            )

        return buildChatResponse(answer, conversationResponse, conversation)
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

    private fun buildChatResponse(
        chatResponse: ChatResponseDto,
        conversationResponse: ConversationResponseVm,
        conversation: ConversationEntity,
    ): ChatResponseVm =
        // Only save reply if it has actual reply
        if (chatResponse.answer != null) {
            val answerEntity =
                messageRepo.save(
                    ChatMessageEntity(
                        content = chatResponse.answer,
                        conversation = conversation,
                        type = Constant.ANSWER_TYPE,
                    ),
                )
            if (chatResponse.citations.isNotEmpty()) {
                saveMessageCitations(answerEntity, chatResponse.citations)
            }
            ChatResponseVm(
                conversationResponse,
                ChatMessageResponseVm(
                    answerEntity.id ?: UUID.randomUUID(),
                    answerEntity.content,
                    answerEntity.createdAt!!,
                    type = Constant.ANSWER_TYPE,
                    medias = emptyList(),
                    citations = chatResponse.citations,
                ),
            )
        } else {
            ChatResponseVm(
                conversationResponse,
                null,
            )
        }

    private fun saveMessageCitations(
        answerEntity: ChatMessageEntity,
        citations: List<CitationDto>,
    ) {
        citations.forEach {
            answerEntity.addCitation(
                chunkId = it.chunkId,
                fileName = it.fileName,
                filePath = it.filePath,
                charStart = it.charStart,
                charEnd = it.charEnd,
            )
        }
        messageRepo.save(answerEntity)
    }
}

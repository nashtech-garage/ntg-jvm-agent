package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.mapper.ChatMessageMapper
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVmImpl
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
    private val historyLimit: Int = 5,
) {
    @Transactional
    fun createConversation(
        chatReq: ChatRequestVm,
        username: String,
    ): ChatResponseVm {
        if (chatReq.conversationId != null) {
            return this.addMessage(chatReq.conversationId, chatReq.question)
        }
        val conversationId = this.createNewConversation(chatReq, username)
        return this.addMessage(conversationId, chatReq.question)
    }

    fun listConversationByUser(username: String): List<ConversationResponseVm> {
        val conversations = this.conversationRepo.listActiveConversationsByUser(username)
        return conversations
    }

    fun listMessageByConversation(conversationId: UUID): List<ChatMessageResponseVm> {
        this.conversationRepo
            .findById(conversationId)
            .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }

        return this.messageRepo.listMessageByConversationId(conversationId)
    }

    private fun createNewConversation(
        chatReq: ChatRequestVm,
        username: String,
    ): UUID {
        val titleSummarize = chatModelService.createSummarize(chatReq.question)

        val conversation =
            ConversationEntity(
                title = titleSummarize ?: chatReq.question,
                username = username,
            )

        val savedConversation =
            conversationRepo.save(conversation)

        val conversationId =
            savedConversation.id
                ?: throw BadRequestException("Can't create new conversation")

        return conversationId
    }

    private fun addMessage(
        conversationId: UUID,
        question: String,
    ): ChatResponseVm {
        val conversation = findConversationOrThrow(conversationId)
        saveQuestion(conversation, question)
        val conversationResponse = buildConversationResponse(conversation)
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

        val summary = chatModelService.createDynamicSummary(olderMessages)

        val answer =
            chatModelService.call(
                message = question,
                history = recentMessages,
                summary = summary,
            )

        return if (answer != null) {
            saveAnswer(conversation, answer, conversationResponse)
        } else {
            ChatResponseVm(conversationResponse, null)
        }
    }

    private fun findConversationOrThrow(conversationId: UUID) =
        conversationRepo
            .findById(conversationId)
            .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }

    private fun saveQuestion(
        conversation: ConversationEntity,
        question: String,
    ): ChatMessageEntity =
        messageRepo.save(
            ChatMessageEntity(
                content = question,
                conversation = conversation,
                type = Constant.QUESTION_TYPE,
            ),
        )

    private fun buildConversationResponse(conversation: ConversationEntity) =
        ConversationResponseVmImpl(
            conversation.id!!,
            conversation.title,
            conversation.createdAt,
        )

    private fun saveAnswer(
        conversation: ConversationEntity,
        answer: String,
        conversationResponse: ConversationResponseVm,
    ): ChatResponseVm {
        val answerMsgEntity =
            messageRepo.save(
                ChatMessageEntity(
                    content = answer,
                    conversation = conversation,
                    type = Constant.ANSWER_TYPE,
                ),
            )

        return ChatResponseVm(
            conversationResponse,
            ChatMessageResponseVmImpl(
                answerMsgEntity.id ?: UUID.randomUUID(),
                answerMsgEntity.content,
                answerMsgEntity.createdAt,
                type = Constant.ANSWER_TYPE,
            ),
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
}

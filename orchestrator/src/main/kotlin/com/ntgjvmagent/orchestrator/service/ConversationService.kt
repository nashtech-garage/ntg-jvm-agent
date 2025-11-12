package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.entity.ConversationSummaryEntity
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
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ConversationService(
    private val chatModelService: ChatModelService,
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
    private val conversationSummaryService: ConversationSummaryService,
    private val historyLimit: Int = 10,
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

        val initialSummary =
            ConversationSummaryEntity(
                conversation = savedConversation,
                summaryText = "",
            )

        conversationSummaryService.saveInitialSummary(initialSummary)

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
                .findByConversationIdOrderByCreatedAtAsc(conversationId, PageRequest.of(0, historyLimit))
                .map { ChatMessageMapper.toHistoryFormat(it) }
        val currentSummary =
            conversationSummaryService
                .getSummary(conversationId)
                .orEmpty()
        val answer =
            chatModelService.call(
                message = question,
                history = history,
                summary = currentSummary,
            )

        return if (answer != null) {
            saveAnswerAndUpdateSummary(conversation, question, answer, conversationResponse)
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

    private fun saveAnswerAndUpdateSummary(
        conversation: ConversationEntity,
        question: String,
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

        conversationSummaryService.updateSummary(
            conversation = conversation,
            latestMessage = "$question\nAssistant: $answer",
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

package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVmImpl
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
        val titleSummarize = this.chatModelService.createSummarize(chatReq.question)
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
        question: String,
    ): ChatResponseVm {
        val conversation =
            this.conversationRepo
                .findById(conversationId)
                .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }
        // Save question into DB first
        val questionMsg =
            ChatMessageEntity(
                content = question,
                conversation = conversation,
                type = Constant.QUESTION_TYPE,
            )
        this.messageRepo.save(questionMsg)
        val conversationResponse: ConversationResponseVm =
            ConversationResponseVmImpl(
                conversationId,
                conversation.title,
                conversation.createdAt,
            )

        val answer: String? = chatModelService.call(question)
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
                ChatMessageResponseVmImpl(
                    answerMsgEntity.id ?: UUID.randomUUID(),
                    answerMsgEntity.content,
                    answerMsgEntity.createdAt,
                    type = Constant.ANSWER_TYPE,
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
        this.conversationRepo.delete(conversation)
    }
}

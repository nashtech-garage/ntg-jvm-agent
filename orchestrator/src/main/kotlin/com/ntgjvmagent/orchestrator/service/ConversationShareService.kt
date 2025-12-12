package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.ConversationShareEntity
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.mapper.ChatMessageMapper
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.repository.ConversationShareRepository
import com.ntgjvmagent.orchestrator.viewmodel.ConversationShareResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ShareConversationRequest
import com.ntgjvmagent.orchestrator.viewmodel.SharedConversationViewVm
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ConversationShareService(
    private val conversationShareRepo: ConversationShareRepository,
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
) {
    companion object {
        private const val SHARE_TOKEN_LENGTH = 32
        private const val SHARE_TOKEN_CHAR_INDEX_RANGE = 62
    }

    private val secureRandom = SecureRandom()

    @Transactional
    fun shareConversation(
        conversationId: UUID,
        request: ShareConversationRequest,
        username: String,
    ): ConversationShareResponseVm {
        val conversation =
            conversationRepo
                .findById(conversationId)
                .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }

        if (conversation.username != username) {
            throw BadRequestException("You don't have permission to share this conversation")
        }

        val shareToken = generateShareToken()
        val expiresAt = Instant.now().plus(request.expiryDays.toLong(), ChronoUnit.DAYS)

        // Get current messages and save their IDs
        val currentMessages = messageRepo.listMessageByConversationId(conversationId)
        val messageIds = currentMessages.mapNotNull { it.id?.toString() }

        // Create and save the share record with message IDs
        val conversationShare =
            ConversationShareEntity(
                conversation = conversation,
                sharedByUsername = username,
                shareToken = shareToken,
                isExpired = false,
                expiresAt = expiresAt,
                sharedMessageIds = messageIds,
            )

        val savedShare = conversationShareRepo.save(conversationShare)

        return ConversationShareResponseVm(
            id = savedShare.id,
            conversationId = conversation.id ?: conversationId,
            conversationTitle = conversation.title,
            sharedByUsername = username,
            shareToken = shareToken,
            isExpired = false,
            expiresAt = expiresAt,
            createdAt = savedShare.createdAt ?: Instant.now(),
        )
    }

    @Transactional
    fun getSharedConversation(shareToken: String): SharedConversationViewVm {
        val share =
            conversationShareRepo
                .findByShareToken(shareToken)
                .orElseThrow { ResourceNotFoundException("Invalid share token") }

        // Check if share is expired
        if (share.isExpired) {
            throw BadRequestException("This share has been revoked")
        }

        if (share.expiresAt != null && Instant.now().isAfter(share.expiresAt)) {
            share.isExpired = true
            conversationShareRepo.save(share)
            throw BadRequestException("This share link has expired")
        }

        val conversation = share.conversation

        // Get shared message IDs from JSONB array
        val sharedMessageIds: List<String> = share.sharedMessageIds

        // Get messages only with saved IDs
        val messages =
            if (sharedMessageIds.isNotEmpty()) {
                messageRepo
                    .listMessageByConversationId(conversation.id ?: UUID.randomUUID())
                    .filter { msg -> msg.id?.toString() in sharedMessageIds }
                    .map { ChatMessageMapper.toResponse(it) }
            } else {
                emptyList()
            }

        return SharedConversationViewVm(
            id = conversation.id ?: error("Conversation ID cannot be null"),
            title = conversation.title,
            createdAt = conversation.createdAt ?: Instant.now(),
            sharedByUsername = share.sharedByUsername,
            messages = messages,
        )
    }

    private fun generateShareToken(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..SHARE_TOKEN_LENGTH)
            .map { allowedChars[secureRandom.nextInt(SHARE_TOKEN_CHAR_INDEX_RANGE)] }
            .joinToString("")
    }
}

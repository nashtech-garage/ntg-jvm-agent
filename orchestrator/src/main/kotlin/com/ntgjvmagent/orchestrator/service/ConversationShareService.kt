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
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ConversationShareService(
    private val conversationShareRepo: ConversationShareRepository,
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
) {
    companion object {
        private const val SHARE_TOKEN_LENGTH = 32
    }
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
        val expiresAt = OffsetDateTime.now().plusDays(request.expiryDays.toLong())

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
            createdAt = savedShare.createdAt ?: OffsetDateTime.now(),
        )
    }

    @Transactional
    fun revokeShare(
        shareToken: String,
        username: String,
    ): ConversationShareResponseVm {
        val share =
            conversationShareRepo
                .findByShareToken(shareToken)
                .orElseThrow { ResourceNotFoundException("Share not found") }

        if (share.sharedByUsername != username) {
            throw BadRequestException("You don't have permission to revoke this share")
        }

        share.isExpired = true
        val updatedShare = conversationShareRepo.save(share)

        return toResponseVm(updatedShare)
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

        if (share.expiresAt != null && OffsetDateTime.now().isAfter(share.expiresAt)) {
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
            id = conversation.id ?: UUID.randomUUID(),
            title = conversation.title,
            createdAt = conversation.createdAt ?: OffsetDateTime.now(),
            sharedByUsername = share.sharedByUsername,
            messages = messages,
        )
    }

    fun listSharesByConversation(
        conversationId: UUID,
        username: String,
    ): List<ConversationShareResponseVm> {
        // Verify user owns the conversation
        val conversation =
            conversationRepo
                .findById(conversationId)
                .orElseThrow { ResourceNotFoundException("Conversation not found: $conversationId") }

        if (conversation.username != username) {
            throw BadRequestException("You don't have permission to view shares for this conversation")
        }

        val shares = conversationShareRepo.findActiveSharesByConversationId(conversationId)
        return shares.map { toResponseVm(it) }
    }

    private fun toResponseVm(share: ConversationShareEntity): ConversationShareResponseVm =
        ConversationShareResponseVm(
            id = share.id ?: UUID.randomUUID(),
            conversationId = share.conversation.id ?: UUID.randomUUID(),
            conversationTitle = share.conversation.title,
            sharedByUsername = share.sharedByUsername,
            shareToken = share.shareToken,
            isExpired = share.isExpired,
            expiresAt = share.expiresAt,
            createdAt = share.createdAt ?: OffsetDateTime.now(),
        )

    private fun generateShareToken(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..SHARE_TOKEN_LENGTH).map { allowedChars.random() }.joinToString("")
    }
}

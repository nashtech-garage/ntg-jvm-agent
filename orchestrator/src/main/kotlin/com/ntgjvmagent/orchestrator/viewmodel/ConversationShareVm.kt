package com.ntgjvmagent.orchestrator.viewmodel

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.OffsetDateTime
import java.util.UUID

// Request Models
data class ShareConversationRequest(
    @field:Min(value = 1, message = "Expiry days must be at least 1")
    @field:Max(value = 90, message = "Expiry days cannot exceed 90")
    val expiryDays: Int = 7,
)

data class RevokeShareRequest(
    @field:NotBlank(message = "Share token is required")
    val shareToken: String,
)

// Response Models
data class ConversationShareResponseVm(
    val id: UUID?,
    val conversationId: UUID,
    val conversationTitle: String,
    val sharedByUsername: String,
    val shareToken: String,
    val isExpired: Boolean,
    val expiresAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
)

data class ShareLinkResponseVm(
    val id: UUID,
    val shareToken: String,
    val shareUrl: String,
    val expiresAt: OffsetDateTime?,
    val isExpired: Boolean,
)

data class SharedConversationViewVm(
    val id: UUID,
    val title: String,
    val createdAt: OffsetDateTime,
    val sharedByUsername: String,
    val messages: List<ChatMessageResponseVm>,
)


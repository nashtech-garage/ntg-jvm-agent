package com.ntgjvmagent.orchestrator.viewmodel

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant
import java.util.UUID

// Request Models
data class ShareConversationRequest(
    @field:Min(value = 1, message = "Expiry days must be at least 1")
    @field:Max(value = 90, message = "Expiry days cannot exceed 90")
    val expiryDays: Int = 7,
)

// Response Models
data class ConversationShareResponseVm(
    val id: UUID?,
    val conversationId: UUID,
    val conversationTitle: String,
    val sharedByUsername: String,
    val shareToken: String,
    val isExpired: Boolean,
    val expiresAt: Instant?,
    val createdAt: Instant,
)

data class SharedConversationViewVm(
    val id: UUID,
    val title: String,
    val createdAt: Instant,
    val sharedByUsername: String,
    val messages: List<ChatMessageResponseVm>,
)

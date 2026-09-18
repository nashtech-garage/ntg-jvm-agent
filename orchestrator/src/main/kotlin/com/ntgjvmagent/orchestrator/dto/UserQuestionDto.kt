package com.ntgjvmagent.orchestrator.dto

import java.time.Instant
import java.util.UUID

data class UserQuestionOptionDto(
    val label: String,
    val description: String,
)

data class UserQuestionDto(
    val question: String,
    val header: String,
    val options: List<UserQuestionOptionDto>,
    val multiSelect: Boolean,
)

data class PendingQuestionDto(
    val id: UUID,
    val conversationId: UUID?,
    val questions: List<UserQuestionDto>,
    val expiresAt: Instant,
)

data class PendingQuestionResolution(
    val sessionId: UUID,
    val agentId: UUID,
    val prompt: String,
)

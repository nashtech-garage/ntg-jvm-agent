package com.ntgjvmagent.orchestrator.dto.request

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class ConversationIntentRequestDto(
    val agentId: UUID,
    @field:NotBlank(message = "Question must not be blank")
    val question: String,
    val correlationId: String = "",
)

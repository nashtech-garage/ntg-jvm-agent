package com.ntgjvmagent.orchestrator.viewmodel

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class ChatRequestVm(
    @field:NotBlank(message = "Question must not be blank")
    val question: String,
    val conversationId: UUID?,
    @field:NotBlank(message = "Username must not be blank")
    val username: String,
)

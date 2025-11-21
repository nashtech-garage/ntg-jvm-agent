package com.ntgjvmagent.orchestrator.viewmodel

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ConversationUpdateRequestVm(
    @field:NotBlank(message = "Title cannot be blank")
    @field:Size(max = 255, message = "Title cannot exceed 255 characters")
    val title: String,
)

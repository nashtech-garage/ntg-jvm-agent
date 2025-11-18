package com.ntgjvmagent.orchestrator.viewmodel

import jakarta.validation.constraints.NotBlank

data class ConversationUpdateRequestVm(
    @field:NotBlank(message = "Title cannot be blank")
    val title: String,
)


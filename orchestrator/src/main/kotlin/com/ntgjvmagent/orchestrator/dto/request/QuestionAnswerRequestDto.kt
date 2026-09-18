package com.ntgjvmagent.orchestrator.dto.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class QuestionAnswerRequestDto(
    @field:NotNull
    val questionId: UUID,
    @field:NotEmpty
    val answers: Map<String, String>,
)

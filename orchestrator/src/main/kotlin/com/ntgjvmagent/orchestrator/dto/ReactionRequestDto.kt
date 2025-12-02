package com.ntgjvmagent.orchestrator.dto

import com.ntgjvmagent.orchestrator.entity.enums.MessageReaction
import org.jetbrains.annotations.NotNull

data class ReactionRequestDto(
    @field:NotNull
    val reaction: MessageReaction,
)

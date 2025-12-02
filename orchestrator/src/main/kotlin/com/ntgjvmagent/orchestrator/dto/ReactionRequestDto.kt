package com.ntgjvmagent.orchestrator.dto

import com.ntgjvmagent.orchestrator.entity.enums.MessageReaction

data class ReactionRequestDto(
    val reaction: MessageReaction,
)

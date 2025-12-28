package com.ntgjvmagent.orchestrator.utils

import com.ntgjvmagent.orchestrator.model.NormalizedTokenUsage
import org.springframework.ai.chat.metadata.Usage

fun Usage.toNormalizedUsage(): NormalizedTokenUsage =
    NormalizedTokenUsage(
        promptTokens = this.promptTokens?.toLong() ?: 0L,
        completionTokens = this.completionTokens?.toLong() ?: 0L,
    )

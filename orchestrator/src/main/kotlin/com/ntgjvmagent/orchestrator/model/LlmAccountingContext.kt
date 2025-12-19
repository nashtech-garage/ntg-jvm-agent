package com.ntgjvmagent.orchestrator.model

import java.util.UUID

data class LlmAccountingContext(
    val userId: UUID,
    val agentId: UUID,
    val operation: TokenOperation,
    val model: String,
    val inputText: String,
    val outputText: String,
    val estimatedInputTokens: Int,
    val correlationId: String?,
)

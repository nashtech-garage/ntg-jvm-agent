package com.ntgjvmagent.orchestrator.token.accounting

import com.ntgjvmagent.orchestrator.model.TokenOperation
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

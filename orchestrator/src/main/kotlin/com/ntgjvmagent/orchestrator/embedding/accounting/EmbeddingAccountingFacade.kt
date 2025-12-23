package com.ntgjvmagent.orchestrator.embedding.accounting

import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.accounting.TokenMeteringService
import com.ntgjvmagent.orchestrator.token.estimation.TokenEstimatorSelector
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EmbeddingAccountingFacade(
    private val tokenEstimatorSelector: TokenEstimatorSelector,
    private val tokenMeteringService: TokenMeteringService,
) {
    /**
     * Best-effort accounting for embedding usage.
     * Never throws. Never blocks embedding execution.
     */
    fun record(
        agentId: UUID,
        model: String,
        input: Any,
        correlationId: String,
    ) {
        // Embedding operations burn prompt/input tokens only, and never completion/output tokens.
        // Most embedding APIs do NOT return usage metadata
        // So estimation is the only practical option today.
        val estimatedPromptTokens =
            runCatching {
                estimatePromptTokens(model, input)
            }.getOrElse {
                fallbackEstimate(input)
            }

        // Embeddings never generate tokens
        val estimatedCompletionTokens = 0

        val total = estimatedPromptTokens + estimatedCompletionTokens
        if (total <= 0) return

        tokenMeteringService.recordEstimated(
            userId = null, // embeddings may run without user context
            agentId = agentId,
            operation = TokenOperation.EMBEDDING,
            estimatedPromptTokens = estimatedPromptTokens,
            estimatedCompletionTokens = estimatedCompletionTokens,
            correlationId = correlationId,
        )
    }

    private fun estimatePromptTokens(
        model: String,
        input: Any,
    ): Int {
        val estimator = tokenEstimatorSelector.select(model)

        return when (input) {
            is String ->
                estimator.estimateInputTokens(
                    model = model,
                    systemPrompt = "",
                    userPrompt = input,
                    history = emptyList(),
                    summary = "",
                )

            is List<*> ->
                input
                    .filterIsInstance<String>()
                    .sumOf {
                        estimator.estimateInputTokens(
                            model = model,
                            systemPrompt = "",
                            userPrompt = it,
                            history = emptyList(),
                            summary = "",
                        )
                    }

            else -> 0
        }
    }

    /**
     * Very coarse fallback:
     * ~1 token per 4 characters.
     * Used only when estimator fails.
     */
    private fun fallbackEstimate(input: Any): Int =
        when (input) {
            is String ->
                input.length / FALLBACK_CHARS_PER_TOKEN

            is List<*> ->
                input
                    .filterIsInstance<String>()
                    .sumOf { it.length / FALLBACK_CHARS_PER_TOKEN }

            else -> 0
        }

    private companion object {
        private const val FALLBACK_CHARS_PER_TOKEN = 4
    }
}

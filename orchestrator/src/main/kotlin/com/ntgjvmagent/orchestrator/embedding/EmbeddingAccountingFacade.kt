package com.ntgjvmagent.orchestrator.embedding

import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.TokenEstimatorSelector
import com.ntgjvmagent.orchestrator.token.TokenMeteringService
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
        val estimatedTokens =
            runCatching {
                estimateWithModel(model, input)
            }.getOrElse {
                fallbackEstimate(input)
            }

        if (estimatedTokens <= 0) return

        tokenMeteringService.recordEstimated(
            userId = null,
            agentId = agentId,
            operation = TokenOperation.EMBEDDING,
            estimatedTokens = estimatedTokens,
            correlationId = correlationId,
        )
    }

    private fun estimateWithModel(
        model: String,
        input: Any,
    ): Int {
        val estimator = tokenEstimatorSelector.select(model)

        return when (input) {
            is String ->
                estimator.estimateOutputTokens(model, input)

            is List<*> ->
                input
                    .filterIsInstance<String>()
                    .sumOf { estimator.estimateOutputTokens(model, it) }

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

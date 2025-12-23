package com.ntgjvmagent.orchestrator.token.estimation

import org.springframework.stereotype.Component

@Component
class HeuristicTokenEstimator : TokenEstimator {
    override fun estimateInputTokens(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<String>,
        summary: String,
    ): Int {
        val fullText =
            buildString {
                append(systemPrompt).append('\n')
                if (summary.isNotBlank()) append(summary).append('\n')
                history.forEach { append(it).append('\n') }
                append(userPrompt)
            }

        return estimateTokens(fullText)
    }

    override fun estimateOutputTokens(
        model: String,
        text: String,
    ): Int = estimateTokens(text)

    /**
     * Conservative heuristic:
     * ~1 token per 4 characters (English-safe)
     */
    private fun estimateTokens(text: String): Int =
        (text.length / CHARS_PER_TOKEN_ESTIMATE)
            .coerceAtLeast(MIN_ESTIMATED_TOKENS)

    companion object {
        private const val CHARS_PER_TOKEN_ESTIMATE = 4
        private const val MIN_ESTIMATED_TOKENS = 1
    }
}

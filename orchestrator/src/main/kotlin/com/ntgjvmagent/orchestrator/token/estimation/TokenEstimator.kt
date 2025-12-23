package com.ntgjvmagent.orchestrator.token.estimation

/**
 * Estimates token usage for a given model.
 *
 * @param model logical model identifier (e.g. "gpt-4o-mini", "claude-3-sonnet")
 *              Some implementations may ignore this parameter.
 */
interface TokenEstimator {
    fun estimateInputTokens(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<String> = emptyList(),
        summary: String = "",
    ): Int

    fun estimateOutputTokens(
        model: String,
        text: String,
    ): Int
}

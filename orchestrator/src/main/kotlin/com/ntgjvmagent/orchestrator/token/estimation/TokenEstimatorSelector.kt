package com.ntgjvmagent.orchestrator.token.estimation

import org.springframework.stereotype.Component

@Component
class TokenEstimatorSelector(
    private val gpt: GptTokenEstimator,
    private val heuristic: HeuristicTokenEstimator,
) {
    fun select(model: String): TokenEstimator =
        when {
            isGptModel(model) -> gpt
            else -> heuristic
        }

    private fun isGptModel(model: String): Boolean =
        model.startsWith("gpt-") ||
            model.startsWith("gpt_") ||
            model.contains("gpt", ignoreCase = true)
}

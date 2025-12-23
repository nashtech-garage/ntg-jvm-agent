package com.ntgjvmagent.orchestrator.token.accounting

import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.estimation.TokenEstimatorSelector
import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TokenAccountingFacade(
    private val tokenMeteringService: TokenMeteringService,
    private val tokenEstimatorSelector: TokenEstimatorSelector,
) {
    /* =========================================================
     * Token estimation
     * ========================================================= */

    fun estimatePromptInput(
        model: String,
        promptText: String,
    ): Int {
        val estimator = tokenEstimatorSelector.select(model)
        return estimator.estimateInputTokens(
            model = model,
            systemPrompt = Constant.SYSTEM_PROMPT,
            userPrompt = promptText,
            history = emptyList(),
            summary = "",
        )
    }

    fun estimateInput(
        model: String,
        combinedPrompt: String,
        history: List<String>,
        summary: String,
    ): Int {
        val estimator = tokenEstimatorSelector.select(model)
        return estimator.estimateInputTokens(
            model = model,
            systemPrompt = Constant.SYSTEM_PROMPT,
            userPrompt = combinedPrompt,
            history = history,
            summary = summary,
        )
    }

    fun estimateOutput(
        model: String,
        text: String,
    ): Int {
        val estimator = tokenEstimatorSelector.select(model)
        return estimator.estimateOutputTokens(model, text)
    }

    /* =========================================================
     * Budget enforcement
     * ========================================================= */

    fun assertInputBudget(
        userId: UUID,
        operation: TokenOperation,
        estimatedInputTokens: Int,
    ) {
        when (operation) {
            TokenOperation.CHAT ->
                tokenMeteringService.assertWithinBudget(userId, estimatedInputTokens)

            TokenOperation.SUMMARIZATION ->
                tokenMeteringService.assertWithinBudget(
                    userId,
                    // system overhead is cheaper but still bounded
                    (estimatedInputTokens * SUMMARY_WEIGHT).toInt(),
                )

            TokenOperation.EMBEDDING -> Unit
            TokenOperation.TOOL -> Unit
            TokenOperation.RERANK -> Unit
        }
    }

    /* =========================================================
     * Accounting (REAL or FALLBACK)
     * ========================================================= */

    fun recordWithFallback(
        ctx: LlmAccountingContext,
        response: ChatResponse,
    ) {
        val usage = response.metadata.usage

        // Prefer real provider usage
        if (usage != null && isMeaningfulUsage(usage)) {
            tokenMeteringService.record(
                userId = ctx.userId,
                agentId = ctx.agentId,
                operation = ctx.operation,
                usage = usage,
                toolName = null,
                correlationId = ctx.correlationId,
            )
            return
        }

        // Fallback to estimation
        val estimatedOutputTokens =
            estimateOutput(ctx.model, ctx.outputText)

        tokenMeteringService.recordEstimated(
            userId = ctx.userId,
            agentId = ctx.agentId,
            operation = ctx.operation,
            estimatedPromptTokens = ctx.estimatedInputTokens,
            estimatedCompletionTokens = estimatedOutputTokens,
            correlationId = ctx.correlationId,
        )
    }

    /* =========================================================
     * Helpers
     * ========================================================= */

    private fun isMeaningfulUsage(usage: Usage): Boolean =
        usage.totalTokens > 0 ||
            usage.promptTokens > 0 ||
            usage.completionTokens > 0

    companion object {
        private const val SUMMARY_WEIGHT = 0.5
    }
}

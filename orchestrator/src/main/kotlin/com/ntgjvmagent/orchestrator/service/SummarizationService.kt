package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.accounting.LlmAccountingContext
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SummarizationService(
    private val dynamicChatModelService: DynamicChatModelService,
    private val tokenFacade: TokenAccountingFacade,
) {
    fun create(
        userId: UUID,
        agentId: UUID,
        correlationId: String,
        question: String,
    ): String? {
        val promptText =
            """
            ${Constant.SUMMARY_PROMPT}
            "$question"
            """.trimIndent()

        return runSummarization(
            userId = userId,
            agentId = agentId,
            promptText = promptText,
            correlationId = correlationId,
        )
    }

    fun update(
        userId: UUID,
        agentId: UUID,
        correlationId: String,
        messagesToSummarize: List<String>,
    ): String {
        if (messagesToSummarize.isEmpty()) return ""

        val promptText =
            Constant.SUMMARY_UPDATE_PROMPT
                .replace("{{latest_message}}", messagesToSummarize.joinToString("\n"))

        return runSummarization(
            userId = userId,
            agentId = agentId,
            promptText = promptText,
            correlationId = correlationId,
        ).orEmpty()
    }

    private fun runSummarization(
        userId: UUID,
        agentId: UUID,
        promptText: String,
        correlationId: String,
    ): String? {
        val agentConfig = dynamicChatModelService.getAgentConfig(agentId)

        val estimatedInputTokens =
            tokenFacade.estimatePromptInput(
                model = agentConfig.model,
                promptText = promptText,
            )

        tokenFacade.assertInputBudget(
            userId = userId,
            operation = TokenOperation.SUMMARIZATION,
            estimatedInputTokens = estimatedInputTokens,
        )

        val chatModel = dynamicChatModelService.getChatModel(agentId)
        val response = chatModel.call(Prompt(promptText))

        val output =
            response.result
                ?.output
                ?.text
                .orEmpty()

        tokenFacade.recordWithFallback(
            ctx =
                LlmAccountingContext(
                    userId = userId,
                    agentId = agentId,
                    operation = TokenOperation.SUMMARIZATION,
                    model = agentConfig.model,
                    inputText = promptText,
                    outputText = output,
                    estimatedInputTokens = estimatedInputTokens,
                    correlationId = correlationId,
                ),
            response = response,
        )

        return output.ifBlank { null }
    }
}

package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.component.PromptBuilder
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.accounting.LlmAccountingContext
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.util.UUID

@Service
class ChatModelService(
    private val chatStreamService: ChatStreamService,
    private val summarizationService: SummarizationService,
    private val promptBuilder: PromptBuilder,
    private val dynamicModelService: DynamicModelService,
    private val tokenFacade: TokenAccountingFacade,
) {
    fun call(
        userId: UUID,
        request: ChatRequestDto,
        history: List<String> = emptyList(),
        summary: String = "",
    ): Flux<String> {
        val combinedPrompt = promptBuilder.build(request, summary)
        val agentConfig = dynamicModelService.getAgentConfig(request.agentId)

        // Estimate + enforce CHAT input budget
        val estimatedInputTokens =
            tokenFacade.estimateInput(
                model = agentConfig.model,
                combinedPrompt = combinedPrompt,
                history = history,
                summary = summary,
            )

        tokenFacade.assertInputBudget(
            userId = userId,
            operation = TokenOperation.CHAT,
            estimatedInputTokens = estimatedInputTokens,
        )

        val accountingContext =
            LlmAccountingContext(
                userId = userId,
                agentId = request.agentId,
                operation = TokenOperation.CHAT,
                model = agentConfig.model,
                inputText = combinedPrompt,
                outputText = "",
                estimatedInputTokens = estimatedInputTokens,
                correlationId = request.correlationId,
            )

        return chatStreamService.stream(
            userId = userId,
            request = request,
            accountingContext = accountingContext,
        )
    }

    fun createSummarize(
        userId: UUID,
        agentId: UUID,
        correlationId: String,
        question: String,
    ): String? = summarizationService.create(userId, agentId, correlationId, question)

    fun createDynamicSummary(
        userId: UUID,
        agentId: UUID,
        correlationId: String,
        messages: List<String>,
    ): String = summarizationService.update(userId, agentId, correlationId, messages)
}

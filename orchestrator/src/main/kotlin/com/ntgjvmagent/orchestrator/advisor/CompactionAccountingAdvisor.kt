package com.ntgjvmagent.orchestrator.advisor

import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.accounting.LlmAccountingContext
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.content.MediaContent
import org.springframework.ai.tokenizer.TokenCountEstimator
import org.springframework.core.Ordered
import java.util.UUID

class CompactionAccountingAdvisor(
    private val userId: UUID,
    private val agentId: UUID,
    private val model: String,
    private val rootCorrelationId: String,
    private val tokenFacade: TokenAccountingFacade,
    private val tokenCountEstimator: TokenCountEstimator,
) : CallAdvisor {
    override fun adviseCall(
        request: ChatClientRequest,
        chain: CallAdvisorChain,
    ): ChatClientResponse {
        val instructions = request.prompt().instructions
        val estimatedInputTokens = estimateInput(instructions)

        tokenFacade.assertInputBudget(
            userId = userId,
            operation = TokenOperation.SUMMARIZATION,
            estimatedInputTokens = estimatedInputTokens,
        )

        val response = chain.nextCall(request)
        val chatResponse = response.chatResponse() ?: ChatResponse.builder().build()
        val output =
            chatResponse.result
                ?.output
                ?.text
                .orEmpty()

        tokenFacade.recordWithFallback(
            ctx =
                LlmAccountingContext(
                    userId = userId,
                    agentId = agentId,
                    operation = TokenOperation.SUMMARIZATION,
                    model = model,
                    userInputText = instructions.joinToString("\n", transform = ::textOf),
                    outputText = output,
                    estimatedInputTokens = estimatedInputTokens,
                    correlationId = "$rootCorrelationId:compaction",
                ),
            response = chatResponse,
        )
        return response
    }

    /**
     * Estimates every instruction of the prompt, not only the ones implementing [MediaContent].
     *
     * Only `UserMessage` and `AssistantMessage` implement [MediaContent]; `SystemMessage` and
     * [ToolResponseMessage] do not. Filtering on that interface therefore dropped the whole
     * summarization system prompt from the budget check and from the recorded estimate.
     */
    private fun estimateInput(instructions: List<Message>): Int =
        instructions.sumOf { message ->
            when (message) {
                is MediaContent -> tokenCountEstimator.estimate(message)
                else -> tokenCountEstimator.estimate(textOf(message))
            }
        }

    private fun textOf(message: Message): String =
        when (message) {
            is ToolResponseMessage -> {
                message.responses.joinToString("\n") { response ->
                    "${response.name()} ${response.responseData()}"
                }
            }

            else -> {
                message.text.orEmpty()
            }
        }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun getName(): String = javaClass.simpleName
}

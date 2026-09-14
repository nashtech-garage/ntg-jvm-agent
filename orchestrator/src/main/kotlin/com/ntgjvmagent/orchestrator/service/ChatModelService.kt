package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.model.ChatStreamEvent
import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.accounting.LlmAccountingContext
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.session.SessionService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.util.UUID

@Service
class ChatModelService(
    private val chatStreamService: ChatStreamService,
    private val summarizationService: SummarizationService,
    private val dynamicChatModelService: DynamicChatModelService,
    private val tokenFacade: TokenAccountingFacade,
    private val sessionService: SessionService,
    private val agentMemoryService: AgentMemoryService? = null,
) {
    fun call(
        userId: UUID,
        sessionId: UUID,
        request: ChatRequestDto,
    ): Flux<ChatStreamEvent> {
        val agentConfig = dynamicChatModelService.getAgentConfig(request.agentId)
        val history = loadSessionContext(sessionId, userId)
        val memoryIndex = agentMemoryService?.buildIndexForCurrentUser(request.agentId).orEmpty()
        val meteredContext = history + listOfNotNull(memoryIndex.takeIf(String::isNotBlank))

        val estimatedInputTokens =
            tokenFacade.estimateInput(
                model = agentConfig.model,
                userPrompt = request.question,
                history = meteredContext,
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
                userInputText = request.question,
                outputText = "",
                estimatedInputTokens = estimatedInputTokens,
                correlationId = request.correlationId,
            )

        return chatStreamService.stream(
            userId = userId,
            sessionId = sessionId,
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

    private fun loadSessionContext(
        sessionId: UUID,
        userId: UUID,
    ): List<String> {
        val session = sessionService.findById(sessionId.toString()) ?: return emptyList()
        check(session.userId() == userId.toString()) {
            "Session cannot be opened by another user"
        }
        return sessionService.getMessages(sessionId.toString()).map(::messageForEstimation)
    }

    private fun messageForEstimation(message: Message): String =
        when (message) {
            is AssistantMessage -> {
                buildString {
                    append(message.text)
                    message.toolCalls.forEach { toolCall ->
                        append('\n').append(toolCall.name()).append(' ').append(toolCall.arguments())
                    }
                }
            }

            is ToolResponseMessage -> {
                message.responses.joinToString("\n") { response ->
                    "${response.name()} ${response.responseData()}"
                }
            }

            else -> {
                message.text.orEmpty()
            }
        }
}

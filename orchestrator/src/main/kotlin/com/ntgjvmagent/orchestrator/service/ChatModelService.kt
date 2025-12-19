package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.advisor.CallAdvisorRegistry
import com.ntgjvmagent.orchestrator.component.ToolExecutionFacade
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.model.LlmAccountingContext
import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.TokenAccountingFacade
import com.ntgjvmagent.orchestrator.utils.Constant
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class ChatModelService(
    private val toolFacade: ToolExecutionFacade,
    private val dynamicModelService: DynamicModelService,
    private val callAdvisorRegistry: CallAdvisorRegistry,
    private val tokenFacade: TokenAccountingFacade,
) {
    private val logger = LoggerFactory.getLogger(ChatModelService::class.java)

    /* =========================================================
     * CHAT (streaming)
     * ========================================================= */

    fun call(
        userId: UUID,
        request: ChatRequestDto,
        history: List<String> = emptyList(),
        summary: String = "",
    ): Flux<String> {
        val combinedPrompt = buildCombinedPrompt(request, history, summary)
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

        val chatClient = buildChatClient(request.agentId)
        val advisors = callAdvisorRegistry.resolveForAgent(request.agentId)

        return streamChatResponse(
            userId = userId,
            chatClient = chatClient,
            advisors = advisors,
            request = request,
            accountingContext = accountingContext,
        )
    }

    /* =========================================================
     * SUMMARIZATION
     * ========================================================= */

    fun createSummarize(
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

    fun createDynamicSummary(
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

    /* =========================================================
     * Internal helpers
     * ========================================================= */

    private fun runSummarization(
        userId: UUID,
        agentId: UUID,
        promptText: String,
        correlationId: String,
    ): String? {
        val agentConfig = dynamicModelService.getAgentConfig(agentId)

        // Estimate + enforce SUMMARIZATION budget
        val estimatedInputTokens =
            tokenFacade.estimateOutput(agentConfig.model, promptText)

        tokenFacade.assertInputBudget(
            userId = userId,
            operation = TokenOperation.SUMMARIZATION,
            estimatedInputTokens = estimatedInputTokens,
        )

        val chatModel = dynamicModelService.getChatModel(agentId)
        val response = chatModel.call(Prompt(promptText))

        val output =
            response.result
                ?.output
                ?.text
                .orEmpty()

        // Accounting
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

    private fun streamChatResponse(
        userId: UUID,
        chatClient: ChatClient,
        advisors: List<CallAdvisor>,
        request: ChatRequestDto,
        accountingContext: LlmAccountingContext,
    ): Flux<String> {
        val accumulatedText = StringBuilder()
        var finalResponse: ChatResponse? = null

        return chatClient
            .prompt()
            .advisors(advisors)
            .system(
                """
                ${Constant.SYSTEM_PROMPT}
                ${Constant.SEARCH_TOOL_INSTRUCTION}
                """.trimIndent(),
            ).toolCallbacks(
                toolFacade.createToolCallbacks(
                    userId = userId,
                    agentId = request.agentId,
                    correlationId = accountingContext.correlationId!!,
                ),
            ).user { u -> attachUserInput(u, accountingContext.inputText, request) }
            .stream()
            .chatClientResponse()
            .flatMap { event ->
                val response = event.chatResponse ?: return@flatMap Mono.empty()

                finalResponse = response

                val text =
                    response.result
                        ?.output
                        ?.text
                        ?.takeIf { it.isNotBlank() }

                if (text != null) Mono.just(text) else Mono.empty()
            }.doOnNext { chunk ->
                accumulatedText.append(chunk)
            }.doOnComplete {
                tokenFacade.recordWithFallback(
                    ctx =
                        accountingContext.copy(
                            outputText = accumulatedText.toString(),
                        ),
                    response = finalResponse ?: ChatResponse.builder().build(),
                )
            }
    }

    private fun buildChatClient(agentId: UUID): ChatClient =
        ChatClient.builder(dynamicModelService.getChatModel(agentId)).build()

    private fun buildCombinedPrompt(
        request: ChatRequestDto,
        history: List<String>,
        summary: String,
    ): String =
        buildString {
            if (summary.isNotBlank()) {
                appendLine("Conversation summary so far:")
                appendLine(summary)
                appendLine()
            }
            appendLine("User question: ${request.question}")
            if (history.isNotEmpty()) {
                appendLine("Chat history:")
                history.forEach { appendLine(it) }
                appendLine()
            }
        }

    private fun attachUserInput(
        u: ChatClient.PromptUserSpec,
        combinedPrompt: String,
        request: ChatRequestDto,
    ) {
        u.text(combinedPrompt)

        request.files
            ?.filter { !it.isEmpty }
            ?.forEach { file ->
                runCatching {
                    val mime =
                        MimeTypeUtils.parseMimeType(
                            file.contentType ?: Constant.PNG_CONTENT_TYPE,
                        )
                    u.media(mime, InputStreamResource(file.inputStream))
                }.onFailure {
                    logger.warn(
                        "Failed to read file ${file.originalFilename}: ${it.message}",
                    )
                }
            }
    }
}

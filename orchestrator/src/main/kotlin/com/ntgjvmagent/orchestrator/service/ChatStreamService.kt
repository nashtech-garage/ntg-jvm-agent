package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.advisor.CallAdvisorRegistry
import com.ntgjvmagent.orchestrator.advisor.ModelReasoningObservingAdvisor
import com.ntgjvmagent.orchestrator.advisor.SuccessfulSessionRequestAdvisor
import com.ntgjvmagent.orchestrator.advisor.ToolCallObservingAdvisor
import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.component.ToolExecutionFacade
import com.ntgjvmagent.orchestrator.config.ChatReasoningProperties
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.model.ChatStreamEvent
import com.ntgjvmagent.orchestrator.token.accounting.LlmAccountingContext
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import com.ntgjvmagent.orchestrator.utils.Constant
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@Service
class ChatStreamService(
    private val toolFacade: ToolExecutionFacade,
    private val chatClientFactory: AgentChatClientFactory,
    private val callAdvisorRegistry: CallAdvisorRegistry,
    private val tokenFacade: TokenAccountingFacade,
    private val reasoningProperties: ChatReasoningProperties,
    private val agentMemoryService: AgentMemoryService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun stream(
        userId: UUID,
        sessionId: UUID,
        request: ChatRequestDto,
        accountingContext: LlmAccountingContext,
    ): Flux<ChatStreamEvent> {
        val correlationId =
            accountingContext.correlationId
                ?: error("correlationId must not be null")

        val chatClient = chatClientFactory.create(request.agentId)
        val advisors =
            callAdvisorRegistry.resolveForAgent(
                agentId = request.agentId,
                userId = userId,
                rootCorrelationId = correlationId,
            )

        val activityEvents = Sinks.many().unicast().onBackpressureBuffer<ChatStreamEvent>()
        val activityAdvisors = createActivityAdvisors(activityEvents)

        val responses = CopyOnWriteArrayList<ChatResponse>()
        val responseFlux =
            buildSharedResponseFlux(
                sessionId,
                chatClient,
                advisors + activityAdvisors,
                request,
                accountingContext,
            ).doOnNext { event -> event.chatResponse?.let(responses::add) }

        val textStream = buildTextStream(responseFlux)

        val messageEvents: Flux<ChatStreamEvent> =
            textStream
                .map<ChatStreamEvent> { ChatStreamEvent.Message(it) }
                .doOnComplete {
                    recordAccounting(
                        responses,
                        accountingContext,
                        request,
                        userId,
                        correlationId,
                    )
                }.doFinally {
                    activityEvents.tryEmitComplete()
                }

        // Subscribe to the side channel first so synchronous model responses cannot outrun it.
        return Flux.merge(activityEvents.asFlux(), messageEvents)
    }

    private fun createActivityAdvisors(activityEvents: Sinks.Many<ChatStreamEvent>): List<Advisor> {
        val toolCallObserver =
            ToolCallObservingAdvisor { event ->
                val result = activityEvents.tryEmitNext(ChatStreamEvent.Tool(event))
                if (result.isFailure) {
                    logger.debug(
                        "Tool event was not emitted: result={}, toolCallId={}, phase={}",
                        result,
                        event.id,
                        event.phase,
                    )
                }
            }
        if (!reasoningProperties.enabled) return listOf(toolCallObserver)

        val reasoningObserver =
            ModelReasoningObservingAdvisor { content ->
                val result = activityEvents.tryEmitNext(ChatStreamEvent.Reasoning(content))
                if (result.isFailure) {
                    logger.debug("Reasoning event was not emitted: result={}", result)
                }
            }
        return listOf(toolCallObserver, reasoningObserver)
    }

    private fun buildSharedResponseFlux(
        sessionId: UUID,
        chatClient: ChatClient,
        advisors: List<Advisor>,
        request: ChatRequestDto,
        accountingContext: LlmAccountingContext,
    ): Flux<ChatClientResponse> {
        val userId = accountingContext.userId
        val correlationId = requireNotNull(accountingContext.correlationId)
        val memoryIndex = agentMemoryService.buildIndexForCurrentUser(request.agentId)
        val systemPrompt =
            listOf(
                Constant.SYSTEM_PROMPT,
                Constant.SEARCH_TOOL_INSTRUCTION,
                memoryIndex,
            ).filter(String::isNotBlank)
                .joinToString("\n")
        return chatClient
            .prompt()
            .advisors(advisors)
            .advisors { advisorSpec ->
                advisorSpec
                    .param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId.toString())
                    .param(SessionMemoryAdvisor.USER_ID_CONTEXT_KEY, userId.toString())
                    .param(SuccessfulSessionRequestAdvisor.RUN_ID_CONTEXT_KEY, correlationId)
            }.system(systemPrompt)
            .tools(
                *toolFacade
                    .createToolCallbacks(
                        userId = userId,
                        agentId = request.agentId,
                        correlationId = correlationId,
                    ).toTypedArray(),
            ).user { u ->
                attachUserInput(u, accountingContext.userInputText, request)
            }.stream()
            .chatClientResponse()
    }

    private fun buildTextStream(responseFlux: Flux<ChatClientResponse>): Flux<String> =
        responseFlux.flatMap { event ->
            val text =
                event.chatResponse
                    ?.result
                    ?.output
                    ?.text

            if (text.isNullOrBlank()) {
                Mono.empty()
            } else {
                Mono.just(text)
            }
        }

    private fun recordAccounting(
        responses: List<ChatResponse>,
        accountingContext: LlmAccountingContext,
        request: ChatRequestDto,
        userId: UUID,
        correlationId: String,
    ) {
        val lastResponse = responses.lastOrNull()
        val outputText = responses.mapNotNull { it.result?.output?.text }.joinToString("")

        if (responses.isEmpty()) {
            logger.warn(
                "Chat completed without ChatResponse. correlationId={}, agentId={}, userId={}",
                correlationId,
                request.agentId,
                userId,
            )
        }

        tokenFacade.recordWithFallback(
            ctx = accountingContext.copy(outputText = outputText),
            response = lastResponse ?: ChatResponse.builder().build(),
        )
    }

    private fun attachUserInput(
        u: ChatClient.PromptUserSpec,
        userInputText: String,
        request: ChatRequestDto,
    ) {
        u.text(userInputText)

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

package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.advisor.CallAdvisorRegistry
import com.ntgjvmagent.orchestrator.component.ToolExecutionFacade
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.token.accounting.LlmAccountingContext
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import com.ntgjvmagent.orchestrator.utils.Constant
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class ChatStreamService(
    private val toolFacade: ToolExecutionFacade,
    private val dynamicModelService: DynamicModelService,
    private val callAdvisorRegistry: CallAdvisorRegistry,
    private val tokenFacade: TokenAccountingFacade,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun stream(
        userId: UUID,
        request: ChatRequestDto,
        accountingContext: LlmAccountingContext,
    ): Flux<String> {
        val correlationId =
            accountingContext.correlationId
                ?: error("correlationId must not be null")

        val chatClient = buildChatClient(request.agentId)
        val advisors = callAdvisorRegistry.resolveForAgent(request.agentId)

        val responseFlux =
            buildSharedResponseFlux(
                userId,
                chatClient,
                advisors,
                request,
                accountingContext,
                correlationId,
            ).cache() // make response replayable for accounting

        val textStream = buildTextStream(responseFlux)

        val accountingMono =
            buildAccountingMono(
                responseFlux,
                accountingContext,
                request,
                userId,
                correlationId,
            )

        return textStream
            .doFinally {
                // responseFlux is cached, accounting is late subscriber
                accountingMono.subscribe()
            }
    }

    private fun buildSharedResponseFlux(
        userId: UUID,
        chatClient: ChatClient,
        advisors: List<CallAdvisor>,
        request: ChatRequestDto,
        accountingContext: LlmAccountingContext,
        correlationId: String,
    ): Flux<ChatClientResponse> =
        chatClient
            .prompt()
            .advisors(advisors)
            .advisors { advisors ->
                advisors.param(ChatMemory.CONVERSATION_ID, request.conversationId.toString())
            }.system(
                """
                ${Constant.SYSTEM_PROMPT}
                ${Constant.SEARCH_TOOL_INSTRUCTION}
                """.trimIndent(),
            ).toolCallbacks(
                toolFacade.createToolCallbacks(
                    userId = userId,
                    agentId = request.agentId,
                    correlationId = correlationId,
                ),
            ).user { u ->
                attachUserInput(u, accountingContext.inputText, request)
            }.stream()
            .chatClientResponse()

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

    private fun buildAccountingMono(
        responseFlux: Flux<ChatClientResponse>,
        accountingContext: LlmAccountingContext,
        request: ChatRequestDto,
        userId: UUID,
        correlationId: String,
    ): Mono<Unit> =
        responseFlux
            .flatMap { event ->
                event.chatResponse?.let { Mono.just(it) } ?: Mono.empty()
            }.collectList()
            .doOnNext { responses ->
                val lastResponse = responses.lastOrNull()

                val outputText =
                    responses
                        .mapNotNull { it.result?.output?.text }
                        .joinToString("")

                if (responses.isEmpty()) {
                    logger.warn(
                        "Chat completed without ChatResponse. correlationId={}, agentId={}, userId={}",
                        correlationId,
                        request.agentId,
                        userId,
                    )
                }

                tokenFacade.recordWithFallback(
                    ctx =
                        accountingContext.copy(
                            outputText = outputText,
                        ),
                    response =
                        lastResponse
                            ?: ChatResponse.builder().build(),
                )
            }.thenReturn(Unit)

    private fun buildChatClient(agentId: UUID): ChatClient =
        ChatClient.builder(dynamicModelService.getChatModel(agentId)).build()

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

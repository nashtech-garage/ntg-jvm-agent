package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.model.ChatStreamEvent
import org.slf4j.LoggerFactory
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class ConversationStreamingService(
    private val chatModelService: ChatModelService,
    private val commandService: ConversationCommandService,
    private val conversationSessionService: ConversationSessionService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun streamConversation(
        request: ChatRequestDto,
        userId: UUID,
    ): Flux<ServerSentEvent<Any>> {
        // --------------------------------------------------
        // 1. Create correlation identity
        // --------------------------------------------------
        val correlationId = "chat-${UUID.randomUUID()}"

        val correlatedRequest =
            request.copy(
                correlationId = correlationId,
            )

        val sessionId =
            conversationSessionService.resolveSessionId(
                conversationId = correlatedRequest.conversationId,
                userId = userId,
                agentId = correlatedRequest.agentId,
                correlationId = correlationId,
            )

        val answerBuilder = StringBuilder()

        // --------------------------------------------------
        // 2. Stream chat response
        // --------------------------------------------------
        val stream =
            buildChatEventStream(
                userId = userId,
                sessionId = sessionId,
                request = correlatedRequest,
                answerBuilder = answerBuilder,
            )

        // --------------------------------------------------
        // 3. Persist AFTER stream completes
        // --------------------------------------------------
        val completion =
            Mono
                .fromCallable {
                    if (correlatedRequest.conversationId == null) {
                        // First message → create conversation
                        commandService.createConversationWithFirstMessage(
                            userId = userId,
                            chatReq = correlatedRequest,
                            answer = answerBuilder.toString(),
                            sessionId = sessionId,
                        )
                    } else {
                        // Follow-up → append only
                        commandService.appendConversationMessage(
                            userId = userId,
                            chatReq = correlatedRequest,
                            answer = answerBuilder.toString(),
                        )
                    }
                }.map { response ->
                    ServerSentEvent
                        .builder<Any>()
                        .event("complete")
                        .data(response)
                        .build()
                }

        return stream
            .concatWith(completion)
            .onErrorResume { ex -> handleError(ex) }
    }

    // ---------------- helpers ----------------

    private fun buildChatEventStream(
        userId: UUID,
        sessionId: UUID,
        request: ChatRequestDto,
        answerBuilder: StringBuilder,
    ): Flux<ServerSentEvent<Any>> =
        chatModelService
            .call(
                userId = userId,
                sessionId = sessionId,
                request = request,
            ).doOnNext { event ->
                if (event is ChatStreamEvent.Message) {
                    answerBuilder.append(event.content.replace("\r\n", "\n"))
                }
            }.map(::toServerSentEvent)

    private fun toServerSentEvent(event: ChatStreamEvent): ServerSentEvent<Any> =
        when (event) {
            is ChatStreamEvent.Message -> {
                ServerSentEvent
                    .builder<Any>()
                    .event("message")
                    .data(event.content)
                    .build()
            }

            is ChatStreamEvent.Tool -> {
                ServerSentEvent
                    .builder<Any>()
                    .event("tool")
                    .data(event.event)
                    .build()
            }

            is ChatStreamEvent.Reasoning -> {
                ServerSentEvent
                    .builder<Any>()
                    .event("reasoning")
                    .data(event.content)
                    .build()
            }
        }

    private fun handleError(ex: Throwable): Flux<ServerSentEvent<Any>> {
        logger.error("Streaming error", ex)

        return Flux.just(
            ServerSentEvent
                .builder<Any>()
                .event("error")
                .data(
                    mapOf(
                        "message" to "Unexpected error",
                        "details" to ex.message,
                    ),
                ).build(),
        )
    }
}

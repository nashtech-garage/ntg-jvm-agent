package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.mapper.ChatMessageMapper
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class ConversationStreamingService(
    private val chatModelService: ChatModelService,
    private val messageRepo: ChatMessageRepository,
    private val commandService: ConversationCommandService,
    private val historyLimit: Int = 5,
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

        // --------------------------------------------------
        // 2. Load history & generate summary
        // --------------------------------------------------
        val (older, recent) = loadAndSplitHistory(correlatedRequest)

        val summary =
            generateSummary(
                userId = userId,
                agentId = correlatedRequest.agentId,
                correlationId = "$correlationId:summary",
                older = older,
            )

        val answerBuilder = StringBuilder()

        // --------------------------------------------------
        // 3. Stream chat response
        // --------------------------------------------------
        val stream =
            chatModelService
                .call(
                    userId = userId,
                    request = correlatedRequest,
                    history = recent,
                    summary = summary,
                ).doOnNext { token ->
                    answerBuilder.append(token.replace("\r\n", "\n"))
                }.map { token ->
                    ServerSentEvent
                        .builder<Any>()
                        .event("message")
                        .data(token)
                        .build()
                }

        // --------------------------------------------------
        // 4. Persist AFTER stream completes
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

    private fun loadAndSplitHistory(request: ChatRequestDto): Pair<List<String>, List<String>> {
        val history =
            request.conversationId
                ?.let {
                    messageRepo
                        .listMessageByConversationIdOrdered(it)
                        .map(ChatMessageMapper::toHistoryFormat)
                }
                ?: emptyList()

        val split = history.size - historyLimit

        return if (split > 0) {
            history
                .withIndex()
                .partition { it.index < split }
                .let { (o, r) ->
                    o.map { it.value } to r.map { it.value }
                }
        } else {
            emptyList<String>() to history
        }
    }

    private fun generateSummary(
        userId: UUID,
        agentId: UUID,
        correlationId: String,
        older: List<String>,
    ): String =
        if (older.isNotEmpty()) {
            chatModelService.createDynamicSummary(
                userId = userId,
                agentId = agentId,
                correlationId = correlationId,
                messages = older,
            )
        } else {
            ""
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

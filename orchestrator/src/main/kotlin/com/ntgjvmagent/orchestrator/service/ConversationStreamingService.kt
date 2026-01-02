package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.mapper.ChatMessageMapper
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
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
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun streamConversation(
        request: ChatRequestDto,
        userId: UUID,
    ): Flux<ServerSentEvent<Any>> {
        // --------------------------------------------------
        // 1. Ensure conversation exists
        // --------------------------------------------------
        val conversationId =
            request.conversationId ?: commandService.createEmptyConversation()

        // --------------------------------------------------
        // 2. Create correlation identity
        // --------------------------------------------------
        val correlationId = "chat-${UUID.randomUUID()}"

        val correlatedRequest =
            request.copy(
                conversationId = conversationId,
                correlationId = correlationId,
            )

        // --------------------------------------------------
        // 3. Load history & generate summary (same correlation root)
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
        // 4. Stream chat response
        // --------------------------------------------------
        val stream =
            chatModelService
                .call(
                    userId = userId,
                    request = correlatedRequest,
                    history = recent,
                    summary = summary,
                ).doOnNext { answerBuilder.append(it.replace("\r\n", "\n")) }
                .map {
                    ServerSentEvent
                        .builder<Any>()
                        .event("message")
                        .data(it)
                        .build()
                }

        // --------------------------------------------------
        // 5. Finalize message AFTER stream completes
        // --------------------------------------------------
        val completion =
            Mono
                .fromCallable {
                    commandService.appendConversationMessage(
                        userId = userId,
                        chatReq = correlatedRequest,
                        answer = answerBuilder.toString(),
                    )
                }.map {
                    ServerSentEvent
                        .builder<Any>()
                        .event("complete")
                        .data(it)
                        .build()
                }

        return stream
            .concatWith(completion)
            .onErrorResume { ex -> handleError(ex) }
    }

    // ---------------- helpers ----------------

    /**
     * NOTE:
     * At the moment, chat history is queried twice:
     * - once by ChatMemory advisor for history prompting
     * - once by manual token accounting logic
     *
     * Ideally, both should share the same history source.
     * However, the current token accounting implementation depends on
     * a separate, manually controlled history flow.
     *
     * Refactoring this would require non-trivial changes across multiple components,
     * so the duplication is accepted for now.
     */
    private fun loadAndSplitHistory(request: ChatRequestDto): Pair<List<String>, List<String>> =
        request.conversationId?.let { conversationId ->

            val recentMessages =
                messageRepo
                    .findByConversationIdOrderByCreatedAtDesc(
                        conversationId,
                        PageRequest.of(0, Constant.HISTORY_LIMIT),
                    ).asReversed()

            if (recentMessages.isEmpty()) {
                emptyList<String>() to emptyList()
            } else {
                val cutoff =
                    recentMessages.first().createdAt
                        ?: error("ChatMessage.createdAt is null for conversation $conversationId")

                val oldMessages =
                    messageRepo.findMessagesBefore(
                        conversationId = conversationId,
                        cutoff = cutoff,
                    )

                val recentHistory =
                    recentMessages.map(ChatMessageMapper::toHistoryFormat)

                val oldHistory =
                    oldMessages.map(ChatMessageMapper::toHistoryFormat)

                oldHistory to recentHistory
            }
        } ?: run {
            emptyList<String>() to emptyList()
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

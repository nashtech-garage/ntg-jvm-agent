package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.dto.PendingQuestionDto
import com.ntgjvmagent.orchestrator.dto.request.QuestionAnswerRequestDto
import com.ntgjvmagent.orchestrator.model.ChatStreamEvent
import org.slf4j.LoggerFactory
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@Service
class ConversationStreamingService(
    private val chatModelService: ChatModelService,
    private val commandService: ConversationCommandService,
    private val conversationSessionService: ConversationSessionService,
    private val pendingQuestionService: PendingQuestionService? = null,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun streamConversation(
        request: ChatRequestDto,
        userId: UUID,
    ): Flux<ServerSentEvent<Any>> = streamConversation(request, userId, null, true)

    fun answerQuestion(
        conversationId: UUID,
        request: QuestionAnswerRequestDto,
        userId: UUID,
    ): Flux<ServerSentEvent<Any>> {
        val service = requireNotNull(pendingQuestionService)
        val resolution =
            service.beginResolution(
                questionId = request.questionId,
                conversationId = conversationId,
                userId = userId,
                answers = request.answers,
            )
        return Flux
            .defer {
                guardResolutionLifecycle(
                    stream =
                        streamConversation(
                            request =
                                ChatRequestDto(
                                    question = resolution.prompt,
                                    conversationId = conversationId,
                                    files = null,
                                    agentId = resolution.agentId,
                                ),
                            userId = userId,
                            expectedSessionId = resolution.sessionId,
                            cancelPending = false,
                            persistedQuestion = formatAnswers(request.answers),
                        ),
                    service = service,
                    questionId = request.questionId,
                    userId = userId,
                )
            }.doOnError { service.reopenResolution(request.questionId, userId) }
    }

    private fun streamConversation(
        request: ChatRequestDto,
        userId: UUID,
        expectedSessionId: UUID?,
        cancelPending: Boolean,
        persistedQuestion: String? = null,
    ): Flux<ServerSentEvent<Any>> {
        // --------------------------------------------------
        // 1. Create correlation identity
        // --------------------------------------------------
        val correlationId = "chat-${UUID.randomUUID()}"

        val correlatedRequest =
            request.copy(
                correlationId = correlationId,
            )

        if (cancelPending) {
            correlatedRequest.conversationId?.let { conversationId ->
                pendingQuestionService?.cancelForConversation(conversationId, userId)
            }
        }

        val sessionId =
            conversationSessionService.resolveSessionId(
                conversationId = correlatedRequest.conversationId,
                userId = userId,
                agentId = correlatedRequest.agentId,
                correlationId = correlationId,
            )
        check(expectedSessionId == null || expectedSessionId == sessionId) {
            "Pending question session mismatch"
        }

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

        val completion =
            buildCompletion(userId, sessionId, correlatedRequest, persistedQuestion, answerBuilder)

        return stream
            .concatWith(completion)
            .takeUntil { event -> event.event() == "question" }
            .onErrorResume { ex -> handleError(ex) }
    }

    // ---------------- helpers ----------------

    private fun buildCompletion(
        userId: UUID,
        sessionId: UUID,
        request: ChatRequestDto,
        persistedQuestion: String?,
        answerBuilder: StringBuilder,
    ): Mono<ServerSentEvent<Any>> =
        Mono
            .fromCallable {
                when {
                    request.conversationId == null -> {
                        commandService.createConversationWithFirstMessage(
                            userId,
                            request,
                            answerBuilder.toString(),
                            sessionId,
                        )
                    }

                    persistedQuestion == null -> {
                        commandService.appendConversationMessage(userId, request, answerBuilder.toString())
                    }

                    else -> {
                        commandService.appendConversationAnswerTurn(
                            userId,
                            request,
                            persistedQuestion,
                            answerBuilder.toString(),
                        )
                    }
                }
            }.map { response ->
                ServerSentEvent
                    .builder<Any>()
                    .event("complete")
                    .data(response)
                    .build()
            }

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
            ).concatMap { event ->
                if (event is ChatStreamEvent.Question) {
                    Mono.fromCallable {
                        prepareQuestionEvent(userId, sessionId, request, event.question)
                    }
                } else {
                    Mono.just(event)
                }
            }.doOnNext { event ->
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

            is ChatStreamEvent.Question -> {
                ServerSentEvent
                    .builder<Any>()
                    .event("question")
                    .data(event.question)
                    .build()
            }
        }

    private fun prepareQuestionEvent(
        userId: UUID,
        sessionId: UUID,
        request: ChatRequestDto,
        question: PendingQuestionDto,
    ): ChatStreamEvent.Question {
        val conversationId = commandService.persistPendingQuestionTurn(userId, request, sessionId)
        val attached =
            requireNotNull(pendingQuestionService).attachConversation(
                questionId = question.id,
                conversationId = conversationId,
                userId = userId,
            )
        return ChatStreamEvent.Question(attached)
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

    private fun guardResolutionLifecycle(
        stream: Flux<ServerSentEvent<Any>>,
        service: PendingQuestionService,
        questionId: UUID,
        userId: UUID,
    ): Flux<ServerSentEvent<Any>> {
        val transitioned = AtomicBoolean(false)

        fun transition(action: () -> Unit) {
            if (!transitioned.compareAndSet(false, true)) return
            val result = runCatching(action)
            if (result.isFailure) transitioned.set(false)
            result.getOrThrow()
        }

        fun complete() = transition { service.completeResolution(questionId, userId) }

        fun reopen() = transition { service.reopenResolution(questionId, userId) }

        return stream
            .doOnNext { event ->
                when (event.event()) {
                    "complete", "question" -> complete()
                    "error" -> reopen()
                }
            }.doOnError { reopen() }
            .doOnCancel { reopen() }
            .doOnComplete { reopen() }
    }

    private fun formatAnswers(answers: Map<String, String>): String =
        answers.entries.joinToString("\n") { (question, answer) -> "$question: $answer" }
}

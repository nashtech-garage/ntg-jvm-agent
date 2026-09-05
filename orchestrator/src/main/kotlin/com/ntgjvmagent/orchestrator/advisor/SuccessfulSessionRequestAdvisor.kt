package com.ntgjvmagent.orchestrator.advisor

import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.session.SessionEvent
import org.springframework.ai.session.SessionService
import org.springframework.ai.session.advisor.IdempotentSessionEventIdGenerator
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import reactor.core.publisher.Flux

/**
 * Persists the request-side session event only after the model round succeeds.
 *
 * SessionMemoryAdvisor normally appends the user/tool message in before(), which leaves
 * a ghost request in memory if the provider fails. This advisor is ordered immediately
 * inside it: SessionMemoryAdvisor loads history and persists only assistant messages,
 * while this advisor commits the matching user/tool message on successful completion.
 */
class SuccessfulSessionRequestAdvisor(
    private val sessionService: SessionService,
    private val eventIdGenerator: IdempotentSessionEventIdGenerator,
) : CallAdvisor,
    StreamAdvisor {
    companion object {
        const val RUN_ID_CONTEXT_KEY = "session-run-id"
        const val ORDER = ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER + 2
    }

    override fun adviseCall(
        request: ChatClientRequest,
        callAdvisorChain: CallAdvisorChain,
    ): ChatClientResponse {
        val pendingEvent = pendingEvent(request)
        val response = callAdvisorChain.nextCall(request)
        pendingEvent?.let(sessionService::appendEvent)
        return response
    }

    override fun adviseStream(
        request: ChatClientRequest,
        streamAdvisorChain: StreamAdvisorChain,
    ): Flux<ChatClientResponse> {
        val pendingEvent = pendingEvent(request)
        return streamAdvisorChain
            .nextStream(request)
            .doOnComplete { pendingEvent?.let(sessionService::appendEvent) }
    }

    override fun getName(): String = "successful-session-request"

    override fun getOrder(): Int = ORDER

    private fun pendingEvent(request: ChatClientRequest): SessionEvent? {
        val message: Message = request.prompt.lastUserOrToolResponseMessage ?: return null
        val sessionId =
            request.context[SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY] as? String
                ?: error("No session ID found in advisor context")
        return SessionEvent
            .builder()
            .id(eventIdGenerator.generate(request, message))
            .sessionId(sessionId)
            .message(message)
            .build()
    }
}

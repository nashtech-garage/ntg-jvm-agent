package com.ntgjvmagent.orchestrator.advisor

import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog.Companion.TODO_WRITE_TOOL_NAME
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.util.StringUtils
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap

/**
 * Observes the intermediate responses hidden by [org.springframework.ai.chat.client.advisor.ToolCallingAdvisor].
 *
 * This advisor must be instantiated once per chat request because it keeps the call IDs needed
 * to correlate a streamed tool request with the ToolResponseMessage in the next loop iteration.
 */
class ToolCallObservingAdvisor(
    private val listener: (ToolCallEvent) -> Unit,
) : CallAdvisor,
    StreamAdvisor {
    companion object {
        // A larger order places this advisor inside the configured tool-calling advisor loop.
        const val ORDER = ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER + 10
    }

    private val started = ConcurrentHashMap.newKeySet<String>()
    private val completed = ConcurrentHashMap.newKeySet<String>()

    override fun adviseStream(
        chatClientRequest: ChatClientRequest,
        streamAdvisorChain: StreamAdvisorChain,
    ): Flux<ChatClientResponse> {
        emitCompletions(chatClientRequest)
        return streamAdvisorChain
            .nextStream(chatClientRequest)
            .doOnNext(::emitStarts)
    }

    override fun adviseCall(
        chatClientRequest: ChatClientRequest,
        callAdvisorChain: CallAdvisorChain,
    ): ChatClientResponse {
        emitCompletions(chatClientRequest)
        return callAdvisorChain
            .nextCall(chatClientRequest)
            .also(::emitStarts)
    }

    private fun emitStarts(response: ChatClientResponse) {
        response.chatResponse
            ?.results
            .orEmpty()
            .flatMap { it.output.toolCalls }
            .filter { StringUtils.hasText(it.id()) && StringUtils.hasText(it.name()) }
            .filter { started.add(it.id()) }
            .forEach {
                listener(
                    ToolCallEvent(
                        id = it.id(),
                        name = it.name(),
                        phase = ToolCallEvent.Phase.STARTED,
                    ),
                )
            }
    }

    private fun emitCompletions(request: ChatClientRequest) {
        request.prompt.instructions
            .filterIsInstance<ToolResponseMessage>()
            .flatMap { it.responses }
            .filter { started.contains(it.id()) && completed.add(it.id()) }
            .forEach {
                listener(
                    ToolCallEvent(
                        id = it.id(),
                        name = it.name(),
                        phase = ToolCallEvent.Phase.COMPLETED,
                        todoItems =
                            if (it.name() == TODO_WRITE_TOOL_NAME) {
                                ToolCallEvent.todoItemsFrom(it.responseData())
                            } else {
                                null
                            },
                    ),
                )
            }
    }

    override fun getName(): String = "tool-call-observer"

    override fun getOrder(): Int = ORDER
}

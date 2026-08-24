package com.ntgjvmagent.orchestrator.advisor

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap

@Component
class ToolLoopLoggingAdvisor :
    CallAdvisor,
    StreamAdvisor {
    companion object {
        const val ORDER = Ordered.LOWEST_PRECEDENCE - 50
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun adviseCall(
        chatClientRequest: ChatClientRequest,
        callAdvisorChain: CallAdvisorChain,
    ): ChatClientResponse {
        logger.debug("Tool loop model stage started")
        val chatClientResponse = callAdvisorChain.nextCall(chatClientRequest)
        val requestedTools =
            requestedToolNames(chatClientResponse)

        logCompletedStage(requestedTools)
        return chatClientResponse
    }

    override fun adviseStream(
        chatClientRequest: ChatClientRequest,
        streamAdvisorChain: StreamAdvisorChain,
    ): Flux<ChatClientResponse> {
        logger.debug("Tool loop model stage started")
        val requestedTools = ConcurrentHashMap.newKeySet<String>()

        return streamAdvisorChain
            .nextStream(chatClientRequest)
            .doOnNext { requestedTools.addAll(requestedToolNames(it)) }
            .doOnComplete { logCompletedStage(requestedTools.toList().sorted()) }
    }

    private fun requestedToolNames(chatClientResponse: ChatClientResponse): List<String> =
        chatClientResponse.chatResponse
            ?.result
            ?.output
            ?.toolCalls
            .orEmpty()
            .map { it.name() }

    private fun logCompletedStage(requestedTools: List<String>) {
        logger.debug(
            "Tool loop model stage completed: requestedToolCount={}, requestedTools={}",
            requestedTools.size,
            requestedTools,
        )
    }

    override fun getName(): String = "tool-loop-stage"

    override fun getOrder(): Int = ORDER
}

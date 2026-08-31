package com.ntgjvmagent.orchestrator.advisor

import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain
import org.springframework.ai.chat.model.Generation
import reactor.core.publisher.Flux

/**
 * Forwards reasoning text explicitly exposed by a model provider.
 *
 * The advisor runs inside the tool-calling loop so reasoning emitted before an intermediate tool
 * request is not hidden by [org.springframework.ai.chat.client.advisor.ToolCallingAdvisor]. It does
 * not attempt to infer or reconstruct reasoning when the provider does not return it.
 */
class ModelReasoningObservingAdvisor(
    private val listener: (String) -> Unit,
) : CallAdvisor,
    StreamAdvisor {
    companion object {
        // A larger order places this advisor inside the configured tool-calling advisor loop.
        const val ORDER = ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER + 20

        private val OUTPUT_METADATA_KEYS = listOf("reasoningContent", "thinking")
        private val GENERATION_METADATA_KEYS = listOf("thinking", "reasoningContent")
    }

    override fun adviseStream(
        chatClientRequest: ChatClientRequest,
        streamAdvisorChain: StreamAdvisorChain,
    ): Flux<ChatClientResponse> =
        streamAdvisorChain
            .nextStream(chatClientRequest)
            .doOnNext(::emitReasoning)

    override fun adviseCall(
        chatClientRequest: ChatClientRequest,
        callAdvisorChain: CallAdvisorChain,
    ): ChatClientResponse =
        callAdvisorChain
            .nextCall(chatClientRequest)
            .also(::emitReasoning)

    private fun emitReasoning(response: ChatClientResponse) {
        response.chatResponse
            ?.results
            .orEmpty()
            .mapNotNull(::extractReasoning)
            .filter(String::isNotEmpty)
            .forEach(listener)
    }

    private fun extractReasoning(generation: Generation): String? =
        OUTPUT_METADATA_KEYS
            .firstNotNullOfOrNull { key -> generation.output.metadata[key] as? String }
            ?: GENERATION_METADATA_KEYS.firstNotNullOfOrNull { key ->
                if (generation.metadata.containsKey(key)) generation.metadata.get<String>(key) else null
            }

    override fun getName(): String = "model-reasoning-observer"

    override fun getOrder(): Int = ORDER
}

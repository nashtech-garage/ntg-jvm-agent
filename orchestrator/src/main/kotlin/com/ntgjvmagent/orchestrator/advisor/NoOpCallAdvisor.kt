package com.ntgjvmagent.orchestrator.advisor

import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain

/**
 * Fallback advisor that guarantees at least one CallAdvisor exists.
 */
object NoOpCallAdvisor : CallAdvisor {
    override fun adviseCall(
        chatClientRequest: ChatClientRequest,
        callAdvisorChain: CallAdvisorChain,
    ): ChatClientResponse = callAdvisorChain.nextCall(chatClientRequest)

    override fun getName(): String = "no-op"

    override fun getOrder(): Int = Int.MAX_VALUE
}

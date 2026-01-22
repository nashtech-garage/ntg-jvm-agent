package com.ntgjvmagent.orchestrator.advisor

import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CallAdvisorRegistry(
    private val ragAdvisorFactory: RagAdvisorFactory,
) {
    fun resolveForAgent(
        agentId: UUID,
        conversationId: UUID?,
    ): List<CallAdvisor> {
        val advisors = mutableListOf<CallAdvisor>()

        ragAdvisorFactory.createRagAdvisor(agentId)?.let(advisors::add)
        ragAdvisorFactory.createChatMemoryAdvisor(agentId, conversationId)?.let(advisors::add)

        if (advisors.isEmpty()) {
            advisors.add(NoOpCallAdvisor)
        }

        return advisors
    }
}

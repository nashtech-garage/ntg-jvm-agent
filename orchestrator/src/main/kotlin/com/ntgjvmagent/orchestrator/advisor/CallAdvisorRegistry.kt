package com.ntgjvmagent.orchestrator.advisor

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CallAdvisorRegistry(
    private val ragAdvisorFactory: RagAdvisorFactory,
    private val messageChatMemoryAdvisor: MessageChatMemoryAdvisor,
) {
    fun resolveForAgent(agentId: UUID): List<CallAdvisor> {
        val advisors = mutableListOf<CallAdvisor>()

        advisors.add(messageChatMemoryAdvisor)

        ragAdvisorFactory.create(agentId)?.let(advisors::add)

        if (advisors.isEmpty()) {
            advisors.add(NoOpCallAdvisor)
        }

        return advisors
    }
}

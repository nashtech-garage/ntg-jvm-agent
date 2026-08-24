package com.ntgjvmagent.orchestrator.advisor

import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CallAdvisorRegistry(
    private val ragAdvisorFactory: RagAdvisorFactory,
    private val toolLoopLoggingAdvisor: ToolLoopLoggingAdvisor,
) {
    fun resolveForAgent(agentId: UUID): List<Advisor> {
        val advisors = mutableListOf<Advisor>()

        ragAdvisorFactory.create(agentId)?.let(advisors::add)
        advisors.add(toolLoopLoggingAdvisor)

        return advisors
    }
}

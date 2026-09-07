package com.ntgjvmagent.orchestrator.advisor

import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CallAdvisorRegistry(
    private val sessionMemoryAdvisorFactory: SessionMemoryAdvisorFactory,
    private val successfulSessionRequestAdvisor: SuccessfulSessionRequestAdvisor,
    private val ragAdvisorFactory: RagAdvisorFactory,
    private val toolLoopLoggingAdvisor: ToolLoopLoggingAdvisor,
) {
    fun resolveForAgent(
        agentId: UUID,
        userId: UUID,
        rootCorrelationId: String,
    ): List<Advisor> {
        val advisors = mutableListOf<Advisor>()

        advisors.add(sessionMemoryAdvisorFactory.create(agentId, userId, rootCorrelationId))
        advisors.add(successfulSessionRequestAdvisor)
        ragAdvisorFactory.create(agentId)?.let(advisors::add)
        advisors.add(toolLoopLoggingAdvisor)

        return advisors
    }
}

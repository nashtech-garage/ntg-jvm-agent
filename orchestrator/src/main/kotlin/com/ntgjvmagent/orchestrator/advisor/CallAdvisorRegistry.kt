package com.ntgjvmagent.orchestrator.advisor

import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CallAdvisorRegistry(
    private val sessionMemoryAdvisor: SessionMemoryAdvisor,
    private val successfulSessionRequestAdvisor: SuccessfulSessionRequestAdvisor,
    private val ragAdvisorFactory: RagAdvisorFactory,
    private val toolLoopLoggingAdvisor: ToolLoopLoggingAdvisor,
) {
    fun resolveForAgent(agentId: UUID): List<Advisor> {
        val advisors = mutableListOf<Advisor>()

        advisors.add(sessionMemoryAdvisor)
        advisors.add(successfulSessionRequestAdvisor)
        ragAdvisorFactory.create(agentId)?.let(advisors::add)
        advisors.add(toolLoopLoggingAdvisor)

        return advisors
    }
}

package com.ntgjvmagent.orchestrator.advisor

import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.session.MessageFilter
import org.springframework.ai.session.SessionService
import org.springframework.ai.session.advisor.IdempotentSessionEventIdGenerator
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import reactor.core.scheduler.Scheduler
import java.util.UUID

class SessionMemoryAdvisorFactory(
    private val sessionService: SessionService,
    private val dynamicChatModelService: DynamicChatModelService,
    private val compactionFactory: SessionCompactionFactory,
    private val scheduler: Scheduler,
    private val eventIdGenerator: IdempotentSessionEventIdGenerator,
) {
    fun create(
        agentId: UUID,
        userId: UUID,
        rootCorrelationId: String,
    ): SessionMemoryAdvisor {
        val agent = dynamicChatModelService.getAgentConfig(agentId)
        val compaction =
            compactionFactory.create(
                agentId = agentId,
                userId = userId,
                model = agent.model,
                settings = agent.settings,
                rootCorrelationId = rootCorrelationId,
            )
        val builder =
            SessionMemoryAdvisor
                .builder(sessionService)
                .order(ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER + 1)
                .scheduler(scheduler)
                .messageFilter(
                    MessageFilter
                        .byMessageType(MessageType.ASSISTANT)
                        .and(MessageFilter.skipEmptyMessages()),
                ).responseEventIdGenerator(eventIdGenerator)

        if (compaction == null) {
            return builder.build()
        }

        return builder
            .compactionTrigger(compaction.trigger)
            .compactionStrategy(compaction.strategy)
            .build()
    }
}

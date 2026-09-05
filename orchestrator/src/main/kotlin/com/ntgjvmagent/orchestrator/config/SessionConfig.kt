package com.ntgjvmagent.orchestrator.config

import com.ntgjvmagent.orchestrator.advisor.SuccessfulSessionRequestAdvisor
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.session.MessageFilter
import org.springframework.ai.session.SessionService
import org.springframework.ai.session.advisor.IdempotentSessionEventIdGenerator
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SessionConfig {
    @Bean
    fun sessionEventIdGenerator(): IdempotentSessionEventIdGenerator =
        IdempotentSessionEventIdGenerator(
            SuccessfulSessionRequestAdvisor.RUN_ID_CONTEXT_KEY,
            SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY,
        )

    @Bean
    fun sessionMemoryAdvisor(
        sessionService: SessionService,
        eventIdGenerator: IdempotentSessionEventIdGenerator,
    ): SessionMemoryAdvisor =
        SessionMemoryAdvisor
            .builder(sessionService)
            .order(ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER + 1)
            .messageFilter(
                MessageFilter
                    .byMessageType(MessageType.ASSISTANT)
                    .and(MessageFilter.skipEmptyMessages()),
            ).responseEventIdGenerator(eventIdGenerator)
            .build()

    @Bean
    fun successfulSessionRequestAdvisor(
        sessionService: SessionService,
        eventIdGenerator: IdempotentSessionEventIdGenerator,
    ): SuccessfulSessionRequestAdvisor = SuccessfulSessionRequestAdvisor(sessionService, eventIdGenerator)
}

package com.ntgjvmagent.orchestrator.config

import com.ntgjvmagent.orchestrator.advisor.SessionCompactionFactory
import com.ntgjvmagent.orchestrator.advisor.SessionMemoryAdvisorFactory
import com.ntgjvmagent.orchestrator.advisor.SuccessfulSessionRequestAdvisor
import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import com.ntgjvmagent.orchestrator.token.estimation.TokenEstimatorSelector
import org.springframework.ai.session.SessionService
import org.springframework.ai.session.advisor.IdempotentSessionEventIdGenerator
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

@Configuration
class SessionConfig {
    @Bean
    fun sessionEventIdGenerator(): IdempotentSessionEventIdGenerator =
        IdempotentSessionEventIdGenerator(
            SuccessfulSessionRequestAdvisor.RUN_ID_CONTEXT_KEY,
            SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY,
        )

    @Bean
    fun successfulSessionRequestAdvisor(
        sessionService: SessionService,
        eventIdGenerator: IdempotentSessionEventIdGenerator,
    ): SuccessfulSessionRequestAdvisor = SuccessfulSessionRequestAdvisor(sessionService, eventIdGenerator)

    @Bean
    fun sessionCompactionFactory(
        chatClientFactory: AgentChatClientFactory,
        tokenFacade: TokenAccountingFacade,
        tokenEstimatorSelector: TokenEstimatorSelector,
        properties: SessionCompactionProperties,
    ): SessionCompactionFactory =
        SessionCompactionFactory(
            chatClientFactory = chatClientFactory,
            tokenFacade = tokenFacade,
            tokenEstimatorSelector = tokenEstimatorSelector,
            properties = properties,
        )

    @Bean
    fun sessionMemoryAdvisorFactory(
        sessionService: SessionService,
        dynamicChatModelService: DynamicChatModelService,
        sessionCompactionFactory: SessionCompactionFactory,
        sessionMemoryScheduler: Scheduler,
        eventIdGenerator: IdempotentSessionEventIdGenerator,
    ): SessionMemoryAdvisorFactory =
        SessionMemoryAdvisorFactory(
            sessionService = sessionService,
            dynamicChatModelService = dynamicChatModelService,
            compactionFactory = sessionCompactionFactory,
            scheduler = sessionMemoryScheduler,
            eventIdGenerator = eventIdGenerator,
        )

    @Bean(destroyMethod = "dispose")
    fun sessionMemoryScheduler(): Scheduler =
        Schedulers.newBoundedElastic(
            SESSION_MEMORY_MAX_THREADS,
            SESSION_MEMORY_MAX_QUEUED_TASKS,
            "session-memory",
        )

    companion object {
        private const val SESSION_MEMORY_MAX_THREADS = 4
        private const val SESSION_MEMORY_MAX_QUEUED_TASKS = 1_000
    }
}

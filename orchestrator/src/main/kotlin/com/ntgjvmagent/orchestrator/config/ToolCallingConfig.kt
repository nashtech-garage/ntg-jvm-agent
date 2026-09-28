package com.ntgjvmagent.orchestrator.config

import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class ToolCallingConfig {
    companion object {
        private const val TOOL_CALLING_ADVISOR_OFFSET = 100
    }

    @Bean
    fun toolCallingAdvisorBuilder(observationRegistry: ObservationRegistry): ToolCallingAdvisor.Builder<*> =
        ToolCallingAdvisor
            .builder()
            .toolCallingManager(
                ToolCallingManager
                    .builder()
                    .observationRegistry(observationRegistry)
                    .resolutionFallbackEnabled(false)
                    .build(),
            ).advisorOrder(Ordered.LOWEST_PRECEDENCE - TOOL_CALLING_ADVISOR_OFFSET)
}

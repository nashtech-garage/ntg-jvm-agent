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
        const val TOOL_CALLING_ADVISOR_ORDER = Ordered.LOWEST_PRECEDENCE - 100
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
            ).advisorOrder(TOOL_CALLING_ADVISOR_ORDER)
}

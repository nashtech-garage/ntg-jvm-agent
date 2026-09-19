package com.ntgjvmagent.orchestrator.config

import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.tool.IndexAgeEvictionStrategy
import com.ntgjvmagent.orchestrator.tool.SessionScopedVectorToolIndex
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.tool.toolsearch.ToolIndex
import org.springframework.ai.tool.toolsearch.eviction.CompositeEvictionStrategy
import org.springframework.ai.tool.toolsearch.eviction.LruEvictionStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class ToolCallingConfig {
    companion object {
        const val TOOL_CALLING_ADVISOR_ORDER = Ordered.LOWEST_PRECEDENCE - 100
        const val TOOL_SEARCH_MAX_RESULTS = 5
        const val TOOL_SEARCH_SYSTEM_MESSAGE_SUFFIX = """
            You have access to `toolSearchTool`, which discovers tools that can complete the user's request.
            Use it before answering whenever the request depends on a capability or on information that may be
            available through tools. You MUST search before answering questions about the current date, current
            time, time zones, live or real-time information, or application and business data. Do not claim that
            current information or a capability is unavailable until you have searched for a relevant tool.
            For requests such as "What time is it?", search for a current UTC datetime tool and use its result.
        """
    }

    @Bean
    fun toolIndex(vectorStoreService: VectorStoreService): ToolIndex =
        SessionScopedVectorToolIndex(vectorStoreService.getVectorStore())

    @Bean
    fun toolCallingAdvisorBuilder(
        observationRegistry: ObservationRegistry,
        toolIndex: ToolIndex,
        indexProperties: ToolSearchIndexProperties,
    ): ToolCallingAdvisor.Builder<*> =
        ToolSearchToolCallingAdvisor
            .builder()
            .toolIndex(toolIndex)
            .maxResults(TOOL_SEARCH_MAX_RESULTS)
            .systemMessageSuffix(TOOL_SEARCH_SYSTEM_MESSAGE_SUFFIX)
            .conversationHistoryEnabled(false)
            // The age bound is what lets ToolIndexCleanupJob sweep by age safely; the session
            // cap stays as the memory bound the library defaults to.
            .evictionStrategy(
                CompositeEvictionStrategy(
                    IndexAgeEvictionStrategy(indexProperties.maxIndexAge),
                    LruEvictionStrategy(indexProperties.maxSessions),
                ),
            ).toolCallingManager(
                ToolCallingManager
                    .builder()
                    .observationRegistry(observationRegistry)
                    .resolutionFallbackEnabled(false)
                    .build(),
            ).advisorOrder(TOOL_CALLING_ADVISOR_ORDER)
}

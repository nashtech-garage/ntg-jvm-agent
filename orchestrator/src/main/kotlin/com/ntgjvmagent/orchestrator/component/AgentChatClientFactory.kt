package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.config.ToolSearchRoutingProperties
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AgentChatClientFactory(
    private val dynamicChatModelService: DynamicChatModelService,
    private val observationRegistry: ObservationRegistry,
    private val toolCallingAdvisorBuilder: ToolCallingAdvisor.Builder<*>,
    private val toolSearchRoutingProperties: ToolSearchRoutingProperties,
) {
    private val toolSearchAdvisor = toolCallingAdvisorBuilder.build()

    fun create(agentId: UUID): ChatClient = create(agentId, toolCallingAdvisorBuilder, toolSearchAdvisor)

    fun createForToolCatalog(
        agentId: UUID,
        toolCount: Int,
    ): ChatClient =
        if (toolSearchRoutingProperties.shouldUseToolSearch(toolCount)) {
            create(agentId)
        } else {
            createWithoutToolSearch(agentId)
        }

    fun createWithoutToolSearch(agentId: UUID): ChatClient =
        create(
            agentId,
            ToolCallingAdvisor
                .builder()
                .toolCallingManager(
                    ToolCallingManager
                        .builder()
                        .observationRegistry(observationRegistry)
                        .resolutionFallbackEnabled(false)
                        .build(),
                ).advisorOrder(ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER),
            null,
        )

    private fun create(
        agentId: UUID,
        advisorBuilder: ToolCallingAdvisor.Builder<*>,
        advisor: ToolCallingAdvisor?,
    ): ChatClient =
        ChatClient
            .builder(
                dynamicChatModelService.getChatModel(agentId),
                observationRegistry,
                null,
                null,
                advisorBuilder,
            ).apply {
                if (advisor != null) {
                    defaultAdvisors(advisor)
                }
            }.build()
}

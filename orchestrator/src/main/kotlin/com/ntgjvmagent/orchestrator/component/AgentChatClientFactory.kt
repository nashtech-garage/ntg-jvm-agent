package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AgentChatClientFactory(
    private val dynamicChatModelService: DynamicChatModelService,
    private val observationRegistry: ObservationRegistry,
    private val toolCallingAdvisorBuilder: ToolCallingAdvisor.Builder<*>,
) {
    fun create(agentId: UUID): ChatClient =
        ChatClient
            .builder(
                dynamicChatModelService.getChatModel(agentId),
                observationRegistry,
                null,
                null,
                toolCallingAdvisorBuilder,
            ).build()
}

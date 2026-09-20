package com.ntgjvmagent.orchestrator.unit.component

import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.config.ToolSearchIndexProperties
import com.ntgjvmagent.orchestrator.config.ToolSearchRoutingProperties
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import io.micrometer.observation.ObservationRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex
import reactor.core.publisher.Flux
import java.util.UUID
import kotlin.test.assertEquals

class AgentChatClientFactoryToolRoutingTest {
    private val agentId = UUID.randomUUID()

    @Test
    fun `small catalog exposes business tools on the first model stage`() {
        val model = CapturingModel()
        val factory = factory(model)
        val tools = callbacks(9)

        factory
            .createForToolCatalog(agentId, toolCount = tools.size)
            .prompt()
            .advisors { it.param(ChatMemory.CONVERSATION_ID, "small-catalog") }
            .tools(*tools.toTypedArray())
            .user("Acknowledge")
            .call()
            .content()

        assertEquals(tools.map { it.toolDefinition.name() }.sorted(), model.availableToolNames.single())
    }

    @Test
    fun `large catalog exposes progressive discovery on the first model stage`() {
        val model = CapturingModel()
        val factory = factory(model)
        val tools = callbacks(10)

        factory
            .createForToolCatalog(agentId, toolCount = tools.size)
            .prompt()
            .advisors { it.param(ChatMemory.CONVERSATION_ID, "large-catalog") }
            .tools(*tools.toTypedArray())
            .user("Acknowledge")
            .call()
            .content()

        assertEquals(listOf(TOOL_SEARCH_TOOL_NAME), model.availableToolNames.single())
    }

    private fun factory(model: ChatModel): AgentChatClientFactory {
        val dynamicChatModelService = mockk<DynamicChatModelService>()
        every { dynamicChatModelService.getChatModel(agentId) } returns model
        val observationRegistry = ObservationRegistry.NOOP
        return AgentChatClientFactory(
            dynamicChatModelService,
            observationRegistry,
            ToolCallingConfig().toolCallingAdvisorBuilder(
                observationRegistry,
                RegexToolIndex(),
                ToolSearchIndexProperties(),
            ),
            ToolSearchRoutingProperties(minCatalogSize = 10),
        )
    }

    private fun callbacks(count: Int): List<ToolCallback> =
        List(count) { index ->
            val definition = mockk<ToolDefinition>()
            every { definition.name() } returns "tool-$index"
            every { definition.description() } returns "Capability $index"
            every { definition.inputSchema() } returns "{}"
            mockk(relaxed = true) {
                every { toolDefinition } returns definition
            }
        }

    private class CapturingModel : ChatModel {
        val availableToolNames = mutableListOf<List<String>>()

        override fun call(prompt: Prompt): ChatResponse = response(prompt)

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(response(prompt))

        override fun getOptions() = ToolCallingChatOptions.builder().build()

        private fun response(prompt: Prompt): ChatResponse {
            availableToolNames +=
                (prompt.options as ToolCallingChatOptions)
                    .toolCallbacks
                    .orEmpty()
                    .map { it.toolDefinition.name() }
                    .sorted()
            return ChatResponse(listOf(Generation(AssistantMessage("acknowledged"))))
        }
    }

    companion object {
        private const val TOOL_SEARCH_TOOL_NAME = "toolSearchTool"
    }
}

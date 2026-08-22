package com.ntgjvmagent.orchestrator.unit.token

import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.MeteredToolCallback
import com.ntgjvmagent.orchestrator.token.accounting.TokenMeteringService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.ai.tool.metadata.ToolMetadata
import reactor.core.publisher.Flux
import java.util.UUID

class MeteredToolCallbackTest {
    @Test
    fun `delegates tool execution and preserves zero-token accounting event`() {
        val definition = mockk<ToolDefinition>()
        every { definition.name() } returns "clock"
        val delegate = mockk<ToolCallback>()
        every { delegate.toolDefinition } returns definition
        every { delegate.call("{}") } returns "noon"
        val metering = mockk<TokenMeteringService>(relaxed = true)
        val userId = UUID.randomUUID()
        val agentId = UUID.randomUUID()

        val result = MeteredToolCallback(delegate, metering, userId, agentId, "root").call("{}")

        assertEquals("noon", result)
        verify(exactly = 1) {
            metering.record(
                userId = userId,
                agentId = agentId,
                operation = TokenOperation.TOOL,
                usage = match { it.totalTokens == 0 },
                toolName = "clock",
                correlationId = "root:tool:clock",
            )
        }
    }

    @Test
    fun `context-dependent direct tools retain their execution contract through metering`() {
        val meter = mockk<TokenMeteringService>(relaxed = true)
        val model = SingleRoundModel()
        val delegate =
            object : ToolCallback {
                override fun getToolDefinition(): ToolDefinition =
                    ToolDefinition
                        .builder()
                        .name("currentTenant")
                        .description("Return the tenant")
                        .inputSchema("{}")
                        .build()

                override fun getToolMetadata(): ToolMetadata = ToolMetadata.builder().returnDirect(true).build()

                override fun call(arguments: String): String = error("Execution context was dropped")

                override fun call(
                    arguments: String,
                    toolContext: ToolContext?,
                ): String = requireNotNull(toolContext).context.getValue("tenant").toString()
            }
        val callback = MeteredToolCallback(delegate, meter, UUID.randomUUID(), UUID.randomUUID(), "context-test")
        val client = ChatClient.builder(model).defaultAdvisors(ToolCallingAdvisor.builder().build()).build()

        val answer =
            client
                .prompt()
                .tools(callback)
                .toolContext(mapOf("tenant" to "tenant-a"))
                .user("Which tenant?")
                .stream()
                .content()
                .collectList()
                .block()
                .orEmpty()
                .joinToString("")

        assertEquals("tenant-a", answer)
        assertEquals(1, model.calls)
        verify(exactly = 1) { meter.record(any(), any(), any(), any(), any(), any()) }
    }

    private class SingleRoundModel : ChatModel {
        var calls = 0

        override fun getOptions() = ToolCallingChatOptions.builder().build()

        override fun call(prompt: Prompt): ChatResponse {
            check(++calls == 1) { "Return-direct tool incorrectly looped back to the model" }
            val toolCall = AssistantMessage.ToolCall("tenant-call", "function", "currentTenant", "{}")
            val message =
                AssistantMessage
                    .builder()
                    .content("")
                    .toolCalls(listOf(toolCall))
                    .build()
            return ChatResponse(listOf(Generation(message)))
        }

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(call(prompt))
    }
}

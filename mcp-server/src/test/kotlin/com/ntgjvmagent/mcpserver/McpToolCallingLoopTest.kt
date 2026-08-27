package com.ntgjvmagent.mcpserver

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import reactor.core.publisher.Flux
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(McpStreamableRoundTripTest.FixedClockConfig::class)
class McpToolCallingLoopTest(
    @LocalServerPort private val port: Int,
) {
    @Test
    fun `model invokes discovered MCP tool and uses its result in the final answer`() {
        val transport =
            HttpClientStreamableHttpTransport
                .builder("http://127.0.0.1:$port")
                .endpoint("/mcp")
                .build()

        McpClient
            .sync(transport)
            .initializationTimeout(Duration.ofSeconds(5))
            .requestTimeout(Duration.ofSeconds(5))
            .build()
            .use { mcpClient ->
                mcpClient.initialize()
                val callbacks = SyncMcpToolCallbackProvider.syncToolCallbacks(listOf(mcpClient))
                val datetimeCallback = callbacks.single { it.toolDefinition.name() == TOOL_NAME }
                val model = DatetimeToolCallingModel()

                val answer =
                    ChatClient
                        .builder(model)
                        .defaultAdvisors(ToolCallingAdvisor.builder().build())
                        .build()
                        .prompt()
                        .tools(datetimeCallback)
                        .user(CANONICAL_PROMPT)
                        .call()
                        .content()

                assertEquals("The current UTC datetime is 2026-08-30T09:15:00Z.", answer)
                assertEquals(2, model.prompts.size)
                assertTrue(
                    model.prompts
                        .first()
                        .contents
                        .contains(CANONICAL_PROMPT),
                )
                assertTrue(
                    model.receivedToolResult.contains("datetimeUtc") &&
                        model.receivedToolResult.contains("2026-08-30T09:15:00Z"),
                    model.receivedToolResult,
                )
            }
    }

    private class DatetimeToolCallingModel : ChatModel {
        val prompts = mutableListOf<Prompt>()
        var receivedToolResult = ""

        override fun call(prompt: Prompt): ChatResponse = responseFor(prompt)

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(responseFor(prompt))

        override fun getOptions() = ToolCallingChatOptions.builder().build()

        private fun responseFor(prompt: Prompt): ChatResponse {
            prompts.add(prompt)
            val toolResponse = prompt.instructions.filterIsInstance<ToolResponseMessage>().singleOrNull()

            if (toolResponse == null) {
                val toolCall =
                    AssistantMessage.ToolCall(
                        "mcp-datetime-call-1",
                        "function",
                        TOOL_NAME,
                        "{}",
                    )
                return ChatResponse(
                    listOf(
                        Generation(
                            AssistantMessage
                                .builder()
                                .content("")
                                .toolCalls(listOf(toolCall))
                                .build(),
                            ChatGenerationMetadata.builder().finishReason("tool_calls").build(),
                        ),
                    ),
                )
            }

            receivedToolResult = toolResponse.responses.single().responseData()
            val returnedDatetime =
                Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""")
                    .find(receivedToolResult)
                    ?.value
                    ?: error("MCP datetime result did not contain datetimeUtc: $receivedToolResult")
            return ChatResponse(
                listOf(
                    Generation(
                        AssistantMessage("The current UTC datetime is $returnedDatetime."),
                        ChatGenerationMetadata.builder().finishReason("stop").build(),
                    ),
                ),
            )
        }
    }

    companion object {
        private const val TOOL_NAME = "getCurrentDatetime"
        private const val CANONICAL_PROMPT = "What is the current date and time in UTC?"
    }
}

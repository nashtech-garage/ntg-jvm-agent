package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Service

@Service
class ChatModelService(
    private val chatModel: ChatModel,
    private val qaAdvisor: QuestionAnswerAdvisor,
    private val mcpClientToolCallbackProvider: ToolCallbackProvider,
) {
    fun call(
        message: String,
        history: List<String> = emptyList(),
        summary: String,
    ): String? {
        val combinedPrompt =
            buildString {
                if (summary.isNotBlank()) {
                    appendLine("Conversation summary so far:")
                    appendLine(summary)
                    appendLine()
                }

                if (history.isNotEmpty()) {
                    appendLine("Chat history:")
                    history.forEach { item ->
                        appendLine(item)
                    }
                    appendLine()
                }
                appendLine("User: $message")
            }

        val chatClient = ChatClient.builder(chatModel).build()

        val response =
            chatClient
                .prompt()
                .advisors(qaAdvisor)
                .toolCallbacks(mcpClientToolCallbackProvider)
                .user(combinedPrompt)
                .call()
                .content()

        return response
    }

    fun createSummarize(question: String): String? {
        val prompt =
            Prompt(
                """
                ${Constant.SUMMARY_PROMPT}
                "$question"
                """.trimIndent(),
            )

        val response = chatModel.call(prompt)
        return response.result.output.text
    }

    fun runPrompt(promptText: String): String? {
        val prompt = Prompt(promptText)
        val chatClient = ChatClient.builder(chatModel).build()

        val response =
            chatClient
                .prompt(prompt)
                .advisors(qaAdvisor)
                .toolCallbacks(mcpClientToolCallbackProvider)
                .call()

        return response.content()
    }

    fun createDynamicSummary(messagesToSummarize: List<String>): String {
        if (messagesToSummarize.isEmpty()) return ""

        val joinedMessages = messagesToSummarize.joinToString("\n")

        val promptText =
            Constant.SUMMARY_UPDATE_PROMPT
                .replace("{{latest_message}}", joinedMessages)

        val prompt = Prompt(promptText)
        val chatClient = ChatClient.builder(chatModel).build()

        val response =
            chatClient
                .prompt(prompt)
                .advisors(qaAdvisor)
                .toolCallbacks(mcpClientToolCallbackProvider)
                .call()

        return response.content() ?: ""
    }
}

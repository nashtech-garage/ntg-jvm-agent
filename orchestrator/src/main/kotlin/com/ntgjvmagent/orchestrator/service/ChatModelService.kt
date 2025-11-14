package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.viewmodel.ChatRequestVm
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils

@Service
class ChatModelService(
    private val chatModel: ChatModel,
    private val qaAdvisor: QuestionAnswerAdvisor,
    private val mcpClientToolCallbackProvider: ToolCallbackProvider,
) {
    private val logger = LoggerFactory.getLogger(ChatModelService::class.java)

    fun call(
        request: ChatRequestVm,
        history: List<String> = emptyList(),
    ): String? {
        val combinedPrompt =
            buildString {
                history.forEach { item ->
                    appendLine(item)
                }
                appendLine("User: ${request.question}")
            }

        val chatClient = ChatClient.builder(chatModel).build()

        val response =
            chatClient
                .prompt()
                .advisors(qaAdvisor)
                .toolCallbacks(mcpClientToolCallbackProvider)
                .user { u ->
                    u.text(combinedPrompt)
                    request.files
                        ?.filter { !it.isEmpty }
                        ?.forEach { file ->
                            runCatching {
                                val mime =
                                    MimeTypeUtils.parseMimeType(
                                        file.contentType ?: Constant.PNG_CONTENT_TYPE,
                                    )
                                val resource = InputStreamResource(file.inputStream)
                                u.media(mime, resource)
                            }.onFailure { ex ->
                                logger.warn("Failed to read file ${file.originalFilename}: ${ex.message}")
                            }
                        }
                }.call()
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
}

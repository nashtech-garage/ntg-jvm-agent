package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

@Service
class ChatModelService(
    private val chatModel: ChatModel,
) {
    fun call(message: String): String? {
        val prompt = Prompt(message)
        val response = chatModel.call(prompt)
        return response.result.output.text
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

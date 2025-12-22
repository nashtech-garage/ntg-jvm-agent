package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.dto.request.ChatRequestDto
import org.springframework.stereotype.Component

@Component
class PromptBuilder {
    fun build(
        request: ChatRequestDto,
        history: List<String>,
        summary: String,
    ): String =
        buildString {
            if (summary.isNotBlank()) {
                appendLine("Conversation summary so far:")
                appendLine(summary)
                appendLine()
            }
            appendLine("User question: ${request.question}")
            if (history.isNotEmpty()) {
                appendLine("Chat history:")
                history.forEach { appendLine(it) }
                appendLine()
            }
        }
}

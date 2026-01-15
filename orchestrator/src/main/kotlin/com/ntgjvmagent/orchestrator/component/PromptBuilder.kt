package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
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
            if (history.isNotEmpty()) {
                appendLine("Chat history:")
                history.forEach { appendLine(it) }
                appendLine()
            }
            appendLine("User question: ${request.question}")
        }
}

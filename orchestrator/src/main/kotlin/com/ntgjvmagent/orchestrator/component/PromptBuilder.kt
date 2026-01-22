package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import org.springframework.stereotype.Component

@Component
class PromptBuilder {
    fun build(
        request: ChatRequestDto,
        summary: String,
    ): String =
        buildString {
            if (summary.isNotBlank()) {
                appendLine("Conversation summary so far:")
                appendLine(summary)
                appendLine()
            }
            appendLine("User question: ${request.question}")
        }
}

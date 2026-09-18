package com.ntgjvmagent.orchestrator.model

import com.ntgjvmagent.orchestrator.advisor.ToolCallEvent
import com.ntgjvmagent.orchestrator.dto.PendingQuestionDto

sealed interface ChatStreamEvent {
    data class Message(
        val content: String,
    ) : ChatStreamEvent

    data class Tool(
        val event: ToolCallEvent,
    ) : ChatStreamEvent

    data class Reasoning(
        val content: String,
    ) : ChatStreamEvent

    data class Question(
        val question: PendingQuestionDto,
    ) : ChatStreamEvent
}

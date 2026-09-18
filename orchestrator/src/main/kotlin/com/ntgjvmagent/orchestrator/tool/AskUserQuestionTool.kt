package com.ntgjvmagent.orchestrator.tool

import com.ntgjvmagent.orchestrator.dto.PendingQuestionDto
import com.ntgjvmagent.orchestrator.dto.UserQuestionDto
import com.ntgjvmagent.orchestrator.dto.UserQuestionOptionDto
import com.ntgjvmagent.orchestrator.service.PendingQuestionService
import org.springframework.ai.support.ToolCallbacks
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Component
import java.util.UUID
import org.springaicommunity.agent.tools.AskUserQuestionTool as CommunityAskUserQuestionTool

data class AskUserQuestionContext(
    val userId: UUID,
    val agentId: UUID,
    val sessionId: UUID,
    val conversationId: UUID?,
    val correlationId: String,
)

@Component
class AskUserQuestionTool(
    private val pendingQuestionService: PendingQuestionService,
) {
    fun createCallback(
        context: AskUserQuestionContext,
        onQuestion: (PendingQuestionDto) -> Unit,
    ): ToolCallback {
        val tool =
            CommunityAskUserQuestionTool
                .builder()
                .questionHandler { questions ->
                    val pending =
                        pendingQuestionService.create(
                            userId = context.userId,
                            agentId = context.agentId,
                            sessionId = context.sessionId,
                            conversationId = context.conversationId,
                            correlationId = context.correlationId,
                            questions =
                                questions.map { question ->
                                    UserQuestionDto(
                                        question = question.question(),
                                        header = question.header(),
                                        options =
                                            question.options().map { option ->
                                                UserQuestionOptionDto(option.label(), option.description())
                                            },
                                        multiSelect = question.multiSelect(),
                                    )
                                },
                        )
                    onQuestion(pending)
                    questions.associate { it.question() to "Waiting for the user's next turn." }
                }.build()

        return ToolCallbacks.from(tool).single()
    }
}

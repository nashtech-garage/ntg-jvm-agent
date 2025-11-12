package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.entity.ConversationSummaryEntity
import com.ntgjvmagent.orchestrator.repository.ConversationSummaryRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class ConversationSummaryService(
    private val summaryRepo: ConversationSummaryRepository,
    private val chatModelService: ChatModelService,
) {
    @Transactional
    fun updateSummary(
        conversation: ConversationEntity,
        latestMessage: String,
    ) {
        val summaryEntity =
            summaryRepo
                .findById(conversation.id!!)
                .orElseThrow { IllegalStateException("Summary must exist") }

        val prompt =
            Constant.SUMMARY_UPDATE_PROMPT
                .replace("{{old_summary}}", summaryEntity.summaryText)
                .replace("{{latest_message}}", latestMessage)

        val updatedSummary = chatModelService.runPrompt(prompt) ?: summaryEntity.summaryText

        summaryEntity.summaryText = updatedSummary
        summaryEntity.updatedAt = ZonedDateTime.now()

        summaryRepo.save(summaryEntity)
    }

    fun getSummary(conversationId: UUID): String? =
        summaryRepo
            .findById(conversationId)
            .map { it.summaryText }
            .orElse(null)

    fun saveInitialSummary(summary: ConversationSummaryEntity) = summaryRepo.save(summary)
}

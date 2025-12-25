package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.document.Document
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChatMemoryService(
    private val vectorStoreService: VectorStoreService,
    private val MIN_CONTENT_LENGTH: Int = 20,
) {
    fun onMessageSaved(
        agentId: UUID,
        conversationId: UUID,
        role: Int,
        content: String,
    ) {
        if (!shouldEmbed(role, content)) return

        val document =
            Document(
                content,
                mapOf(
                    "agentId" to agentId.toString(),
                    "conversationId" to conversationId.toString(),
                    "role" to role,
                    "type" to "chat_message",
                ),
            )

        vectorStoreService
            .getVectorStore(agentId)
            .add(listOf(document))
    }

    private fun shouldEmbed(
        role: Int,
        content: String,
    ): Boolean {
        val lower = content.lowercase()

        val isEligible =
            role == Constant.QUESTION_TYPE &&
                content.length >= MIN_CONTENT_LENGTH &&
                !isGreetingNoise(lower)

        val hasSignal =
            hasQuestionSignal(content) ||
                hasTechnicalSignal(lower) ||
                hasMemorySignal(lower)

        return isEligible && hasSignal
    }

    private fun isGreetingNoise(lower: String): Boolean = lower in listOf("hi", "hello", "ok", "thanks")

    private fun hasQuestionSignal(content: String): Boolean = '?' in content

    private fun hasTechnicalSignal(lower: String): Boolean {
        val keywords = listOf("thread", "vector", "embedding", "spring", "java")
        return keywords.any(lower::contains)
    }

    private fun hasMemorySignal(lower: String): Boolean {
        val memorySignals =
            listOf(
                "i am ",
                "i’m ",
                "my name is",
                "i work",
                "i prefer",
                "i care about",
            )
        return memorySignals.any(lower::contains)
    }
}

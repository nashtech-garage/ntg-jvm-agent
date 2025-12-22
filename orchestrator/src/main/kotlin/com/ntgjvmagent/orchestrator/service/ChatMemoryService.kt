package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.document.Document
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChatMemoryService(
    private val vectorStoreService: VectorStoreService,
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
        if (role != Constant.QUESTION_TYPE) return false

        if (content.length < 20) return false

        val lower = content.lowercase()

        if (lower in listOf("hi", "hello", "ok", "thanks")) return false

        if ('?' in content) return true

        val keywords = listOf("thread", "vector", "embedding", "spring", "java")
        if (keywords.any { lower.contains(it) }) return true

        val memorySignals =
            listOf(
                "i am ",
                "i’m ",
                "my name is",
                "i work",
                "i prefer",
                "i care about",
            )
        if (memorySignals.any { lower.contains(it) }) return true

        return false
    }
}

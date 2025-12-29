package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.MemoryDecision
import com.ntgjvmagent.orchestrator.enum.MemoryKind
import com.ntgjvmagent.orchestrator.utils.Constant
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.UUID

@Service
class ChatMemoryService(
    private val vectorStoreService: VectorStoreService,
) {
    private val logger = LoggerFactory.getLogger(ChatMemoryService::class.java)

    fun onMessageSaved(
        agentId: UUID,
        conversationId: UUID,
        role: Int,
        content: String,
    ) {
        val decision = scoreMessage(role, content)

        logger.debug(
            "ChatMemoryDecision embed={} score={} kind={} reasons={}",
            decision.embed,
            decision.score,
            decision.kind,
            decision.reasons,
        )

        if (!decision.embed) return

        val document =
            Document(
                content,
                mapOf(
                    "agentId" to agentId.toString(),
                    "conversationId" to conversationId.toString(),
                    "role" to role,
                    "type" to CHAT_MESSAGE_TYPE,
                    "memoryScore" to decision.score,
                    "memoryKind" to decision.kind.name,
                ),
            )

        runCatching {
            vectorStoreService
                .getVectorStore(agentId)
                .add(listOf(document))
        }.onFailure { ex ->
            logger.warn(
                "Failed to vectorize chat memory. agentId={}, conversationId={}",
                agentId,
                conversationId,
                ex,
            )
        }
    }

    @Suppress("LongMethod")
    private fun scoreMessage(
        role: Int,
        content: String,
    ): MemoryDecision {
        val lower = content.lowercase()
        var score = ZERO_SCORE
        val reasons = mutableListOf<String>()
        var kind = MemoryKind.NOISE

        // Hard reject
        if (role != Constant.QUESTION_TYPE) {
            return MemoryDecision(
                embed = false,
                score = ZERO_SCORE,
                kind = MemoryKind.NOISE,
                reasons = listOf(REASON_NOT_USER),
            )
        }

        // Hard accept
        if (hasIntentSignal(lower)) {
            score += INTENT_SCORE
            reasons += REASON_EXPLICIT_INTENT
            kind = MemoryKind.FACT
        }

        // Greeting noise
        if (isGreetingNoise(lower)) {
            score += GREETING_PENALTY
            reasons += REASON_GREETING
        }

        // Length
        if (content.length >= Constant.MIN_CONTENT_LENGTH_FOR_VECTORIZATION) {
            score += LENGTH_SCORE
            reasons += REASON_LENGTH
        }

        // Question
        if (QUESTION_CHAR in content) {
            score += QUESTION_SCORE
            reasons += REASON_QUESTION
            kind = MemoryKind.QUESTION
        }

        // Technical
        if (hasTechnicalSignal(lower)) {
            score += TECHNICAL_SCORE
            reasons += REASON_TECHNICAL
            kind = MemoryKind.TECHNICAL
        }

        // Personal fact
        if (hasFactSignal(lower)) {
            score += FACT_SCORE
            reasons += REASON_FACT
            kind = MemoryKind.FACT
        }

        // Preference
        if (hasPreferenceSignal(lower)) {
            score += PREFERENCE_SCORE
            reasons += REASON_PREFERENCE
            kind = MemoryKind.PREFERENCE
        }

        // Context
        if (hasContextSignal(lower)) {
            score += CONTEXT_SCORE
            reasons += REASON_CONTEXT
            if (kind == MemoryKind.NOISE) {
                kind = MemoryKind.CONTEXT
            }
        }

        val finalScore =
            score
                .coerceIn(MIN_SCORE, MAX_SCORE)
                .let { String.format(Locale.US, SCORE_FORMAT, it).toDouble() }

        val embed = finalScore >= Constant.MEMORY_EMBED_SCORE_THRESHOLD

        return MemoryDecision(
            embed = embed,
            score = finalScore,
            kind = kind,
            reasons = reasons,
        )
    }

    private fun isGreetingNoise(lower: String): Boolean =
        listOf("hi", "hello", "hey", "thanks", "ok", "okay")
            .any { lower == it || lower.startsWith("$it ") }

    private fun hasIntentSignal(lower: String): Boolean =
        listOf(
            "remember this",
            "remember that",
            "please remember",
        ).any(lower::contains)

    private fun hasTechnicalSignal(lower: String): Boolean =
        listOf(
            "java",
            "spring",
            "thread",
            "virtual thread",
            "vector",
            "embedding",
            "jvm",
            "postgres",
            "docker",
        ).any(lower::contains)

    private fun hasFactSignal(lower: String): Boolean =
        listOf(
            "my name is",
            "i am ",
            "i’m ",
            "i work as",
            "i work at",
            "i live in",
            "i was born",
            "i have ",
            "i’ve been",
            "i have been",
        ).any(lower::contains)

    private fun hasPreferenceSignal(lower: String): Boolean =
        listOf(
            "i prefer",
            "i like",
            "i love",
            "i dislike",
            "i hate",
            "i usually",
            "i often",
            "i tend to",
            "i avoid",
        ).any(lower::contains)

    private fun hasContextSignal(lower: String): Boolean =
        listOf(
            "currently",
            "right now",
            "at the moment",
            "these days",
            "for now",
            "temporarily",
            "lately",
            "recently",
        ).any(lower::contains)

    companion object {
        private const val ZERO_SCORE = 0.0
        private const val MIN_SCORE = 0.0
        private const val MAX_SCORE = 1.0

        private const val INTENT_SCORE = 0.60
        private const val GREETING_PENALTY = -0.20
        private const val LENGTH_SCORE = 0.05
        private const val QUESTION_SCORE = 0.15
        private const val TECHNICAL_SCORE = 0.25
        private const val FACT_SCORE = 0.45
        private const val PREFERENCE_SCORE = 0.30
        private const val CONTEXT_SCORE = 0.10

        private const val QUESTION_CHAR = '?'
        private const val SCORE_FORMAT = "%.2f"

        private const val CHAT_MESSAGE_TYPE = "chat_message"

        private const val REASON_NOT_USER = "not_user_message"
        private const val REASON_EXPLICIT_INTENT = "explicit_remember_intent"
        private const val REASON_GREETING = "greeting_penalty"
        private const val REASON_LENGTH = "length"
        private const val REASON_QUESTION = "question"
        private const val REASON_TECHNICAL = "technical"
        private const val REASON_FACT = "personal_fact"
        private const val REASON_PREFERENCE = "preference"
        private const val REASON_CONTEXT = "context"
    }
}

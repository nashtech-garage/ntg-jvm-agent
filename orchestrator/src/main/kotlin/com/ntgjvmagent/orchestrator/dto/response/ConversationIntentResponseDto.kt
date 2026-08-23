package com.ntgjvmagent.orchestrator.dto.response

data class ConversationIntentResponseDto(
    val category: Category,
    val requiresKnowledge: Boolean,
    val confidence: Double,
    val rationale: String,
) {
    enum class Category {
        GENERAL_CHAT,
        KNOWLEDGE_LOOKUP,
        ACCOUNT_ACTION,
    }

    fun normalized(): ConversationIntentResponseDto =
        if (
            confidence in 0.0..1.0 &&
            rationale.isNotBlank() &&
            requiresKnowledge == (category == Category.KNOWLEDGE_LOOKUP)
        ) {
            this
        } else {
            fallback()
        }

    companion object {
        fun fallback(): ConversationIntentResponseDto =
            ConversationIntentResponseDto(
                category = Category.GENERAL_CHAT,
                requiresKnowledge = false,
                confidence = 0.0,
                rationale = "The response could not be classified reliably.",
            )
    }
}

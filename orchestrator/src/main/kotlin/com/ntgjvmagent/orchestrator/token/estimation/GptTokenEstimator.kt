package com.ntgjvmagent.orchestrator.token.estimation

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.EncodingType
import org.springframework.stereotype.Component

@Component
class GptTokenEstimator : TokenEstimator {
    private val encoding =
        Encodings
            .newDefaultEncodingRegistry()
            .getEncoding(EncodingType.CL100K_BASE)

    override fun estimateInputTokens(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<String>,
        summary: String,
    ): Int {
        val fullText =
            buildString {
                append(systemPrompt).append('\n')
                if (summary.isNotBlank()) append(summary).append('\n')
                history.forEach { append(it).append('\n') }
                append(userPrompt)
            }

        return encoding.encode(fullText).size()
    }

    override fun estimateOutputTokens(
        model: String,
        text: String,
    ): Int = encoding.encode(text).size()
}

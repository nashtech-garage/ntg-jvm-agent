package com.ntgjvmagent.orchestrator.token.estimation

import org.springframework.ai.content.MediaContent
import org.springframework.ai.tokenizer.TokenCountEstimator
import java.util.Base64

class SpringAiTokenCountEstimatorAdapter(
    private val model: String,
    selector: TokenEstimatorSelector,
) : TokenCountEstimator {
    private val estimator = selector.select(model)

    override fun estimate(text: String?): Int = text?.let { estimator.estimateOutputTokens(model, it) } ?: 0

    override fun estimate(content: MediaContent): Int =
        estimate(content.text) +
            content.media.sumOf { media ->
                estimate(media.mimeType.toString()) +
                    when (val data = media.data) {
                        is String -> estimate(data)
                        is ByteArray -> estimate(Base64.getEncoder().encodeToString(data))
                        else -> 0
                    }
            }

    override fun estimate(contents: Iterable<MediaContent>): Int = contents.sumOf(::estimate)
}

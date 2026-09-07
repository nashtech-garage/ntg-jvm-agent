package com.ntgjvmagent.orchestrator.unit.token

import com.ntgjvmagent.orchestrator.token.estimation.GptTokenEstimator
import com.ntgjvmagent.orchestrator.token.estimation.HeuristicTokenEstimator
import com.ntgjvmagent.orchestrator.token.estimation.SpringAiTokenCountEstimatorAdapter
import com.ntgjvmagent.orchestrator.token.estimation.TokenEstimatorSelector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.UserMessage

class SpringAiTokenCountEstimatorAdapterTest {
    private val selector = TokenEstimatorSelector(GptTokenEstimator(), HeuristicTokenEstimator())

    @Test
    fun `adapter uses repository estimator for text and messages`() {
        val text = "A compact conversation summary with stable token counting."
        val adapter = SpringAiTokenCountEstimatorAdapter("gpt-4o-mini", selector)
        val expected = selector.select("gpt-4o-mini").estimateOutputTokens("gpt-4o-mini", text)

        assertEquals(expected, adapter.estimate(text))
        assertEquals(expected, adapter.estimate(UserMessage(text)))
        assertEquals(expected * 2, adapter.estimate(listOf(UserMessage(text), UserMessage(text))))
    }
}

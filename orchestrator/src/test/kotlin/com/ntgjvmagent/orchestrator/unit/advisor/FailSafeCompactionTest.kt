package com.ntgjvmagent.orchestrator.unit.advisor

import com.ntgjvmagent.orchestrator.advisor.FailSafeCompactionStrategy
import com.ntgjvmagent.orchestrator.advisor.FailSafeCompactionTrigger
import com.ntgjvmagent.orchestrator.exception.TokenLimitExceededException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.session.Session
import org.springframework.ai.session.SessionEvent
import org.springframework.ai.session.compaction.CompactionRequest
import org.springframework.ai.session.compaction.CompactionResult
import org.springframework.ai.session.compaction.CompactionStrategy
import org.springframework.ai.session.compaction.CompactionTrigger
import java.time.Instant
import java.util.UUID

class FailSafeCompactionTest {
    @Test
    fun `a budget rejection during compaction leaves the event log untouched`() {
        val strategy =
            FailSafeCompactionStrategy(
                CompactionStrategy { throw TokenLimitExceededException("summarization budget exceeded") },
            )

        val result = strategy.compact(request())

        assertEquals(1, result.compactedEvents().size)
        assertTrue(result.archivedEvents().isEmpty())
        assertEquals(0, result.tokensEstimatedSaved())
    }

    @Test
    fun `a failing trigger declines compaction instead of propagating`() {
        val trigger = FailSafeCompactionTrigger(CompactionTrigger { error("estimator blew up") })

        assertFalse(trigger.shouldCompact(request()))
    }

    @Test
    fun `a healthy delegate is passed through untouched`() {
        val delegateResult = CompactionResult(emptyList(), listOf(event()), 42)
        val strategy = FailSafeCompactionStrategy(CompactionStrategy { delegateResult })
        val trigger = FailSafeCompactionTrigger(CompactionTrigger { true })

        assertEquals(delegateResult, strategy.compact(request()))
        assertTrue(trigger.shouldCompact(request()))
    }

    private fun request(): CompactionRequest =
        CompactionRequest.of(
            Session
                .builder()
                .id(SESSION_ID)
                .userId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .build(),
            listOf(event()),
        )

    private fun event(): SessionEvent =
        SessionEvent
            .builder()
            .id("event-1")
            .sessionId(SESSION_ID)
            .message(UserMessage("hello"))
            .build()

    private companion object {
        const val SESSION_ID = "session-1"
    }
}

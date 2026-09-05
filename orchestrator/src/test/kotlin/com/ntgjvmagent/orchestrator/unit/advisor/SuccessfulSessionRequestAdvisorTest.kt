package com.ntgjvmagent.orchestrator.unit.advisor

import com.ntgjvmagent.orchestrator.advisor.SuccessfulSessionRequestAdvisor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.session.SessionService
import org.springframework.ai.session.advisor.IdempotentSessionEventIdGenerator
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import reactor.core.publisher.Flux

class SuccessfulSessionRequestAdvisorTest {
    private val sessionService = mockk<SessionService>(relaxed = true)
    private val advisor =
        SuccessfulSessionRequestAdvisor(
            sessionService,
            IdempotentSessionEventIdGenerator(
                SuccessfulSessionRequestAdvisor.RUN_ID_CONTEXT_KEY,
                SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY,
            ),
        )

    @Test
    fun `provider error does not persist request event`() {
        val chain = mockk<StreamAdvisorChain>()
        every { chain.nextStream(any()) } returns Flux.error(IllegalStateException("provider unavailable"))

        assertThrows(IllegalStateException::class.java) {
            advisor.adviseStream(request(), chain).blockLast()
        }

        verify(exactly = 0) { sessionService.appendEvent(any()) }
    }

    @Test
    fun `client cancellation does not persist request event`() {
        val chain = mockk<StreamAdvisorChain>()
        every { chain.nextStream(any()) } returns Flux.never()

        val subscription = advisor.adviseStream(request(), chain).subscribe()
        subscription.dispose()

        verify(exactly = 0) { sessionService.appendEvent(any()) }
    }

    private fun request(): ChatClientRequest =
        ChatClientRequest(
            Prompt(UserMessage("hello")),
            mapOf(
                SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY to "session-1",
                SuccessfulSessionRequestAdvisor.RUN_ID_CONTEXT_KEY to "run-1",
            ),
        )
}

package com.ntgjvmagent.orchestrator.unit.tool

import com.ntgjvmagent.orchestrator.dto.PendingQuestionDto
import com.ntgjvmagent.orchestrator.dto.UserQuestionDto
import com.ntgjvmagent.orchestrator.dto.UserQuestionOptionDto
import com.ntgjvmagent.orchestrator.service.PendingQuestionService
import com.ntgjvmagent.orchestrator.tool.AskUserQuestionContext
import com.ntgjvmagent.orchestrator.tool.AskUserQuestionTool
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AskUserQuestionToolTest {
    private val pendingQuestionService = mockk<PendingQuestionService>()
    private val context =
        AskUserQuestionContext(
            userId = UUID.randomUUID(),
            agentId = UUID.randomUUID(),
            sessionId = UUID.randomUUID(),
            conversationId = UUID.randomUUID(),
            correlationId = "chat-load",
        )
    private val pending =
        PendingQuestionDto(
            id = UUID.randomUUID(),
            conversationId = context.conversationId,
            questions =
                listOf(
                    UserQuestionDto(
                        question = "Which mode should be used?",
                        header = "Mode",
                        options =
                            listOf(
                                UserQuestionOptionDto("Safe", "Require confirmation"),
                                UserQuestionOptionDto("Fast", "Continue immediately"),
                            ),
                        multiSelect = false,
                    ),
                ),
            expiresAt = Instant.now().plusSeconds(3600),
        )

    @Test
    fun `handler emits and returns without waiting for an answer`() {
        every { pendingQuestionService.create(any(), any(), any(), any(), any(), any()) } returns pending
        val emitted = mutableListOf<PendingQuestionDto>()
        val callback = AskUserQuestionTool(pendingQuestionService).createCallback(context, emitted::add)

        val result =
            assertTimeoutPreemptively(Duration.ofSeconds(1)) {
                callback.call(ARGUMENTS)
            }

        assertEquals(listOf(pending), emitted)
        assertTrue(result.contains("Waiting for the user's next turn"))
    }

    @Test
    fun `many waiting questions do not exhaust a small worker pool`() {
        every { pendingQuestionService.create(any(), any(), any(), any(), any(), any()) } returns pending
        val callback = AskUserQuestionTool(pendingQuestionService).createCallback(context) {}
        val executor = Executors.newFixedThreadPool(4)

        try {
            val futures = (1..200).map { executor.submit<String> { callback.call(ARGUMENTS) } }
            executor.shutdown()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
            assertTrue(futures.all { it.isDone })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `reactive question path contains no indefinite blocking primitive`() {
        val sources =
            listOf(
                Path.of("src/main/kotlin/com/ntgjvmagent/orchestrator/tool/AskUserQuestionTool.kt"),
                Path.of("src/main/kotlin/com/ntgjvmagent/orchestrator/service/ConversationStreamingService.kt"),
            ).joinToString("\n") { Files.readString(it) }

        assertFalse(sources.contains(".block("))
        assertFalse(sources.contains(".get("))
        assertFalse(sources.contains("CountDownLatch"))
        assertFalse(sources.contains("CompletableFuture"))
    }

    private companion object {
        const val ARGUMENTS =
            """{"questions":[{"question":"Which mode should be used?","header":"Mode","options":[{"label":"Safe","description":"Require confirmation"},{"label":"Fast","description":"Continue immediately"}],"multiSelect":false}]}"""
    }
}

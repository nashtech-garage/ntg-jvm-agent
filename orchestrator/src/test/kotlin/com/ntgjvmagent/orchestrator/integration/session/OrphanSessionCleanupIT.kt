package com.ntgjvmagent.orchestrator.integration.session

import com.ntgjvmagent.orchestrator.config.SessionCleanupProperties
import com.ntgjvmagent.orchestrator.entity.Conversation
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.integration.config.TestAuditorConfig
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.service.ConversationCommandService
import com.ntgjvmagent.orchestrator.service.maintenance.OrphanSessionCleanupJob
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.session.CreateSessionRequest
import org.springframework.ai.session.SessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class OrphanSessionCleanupIT
    @Autowired
    constructor(
        private val sessionService: SessionService,
        private val conversationRepository: ConversationRepository,
        private val commandService: ConversationCommandService,
        private val jdbcTemplate: JdbcTemplate,
    ) : BaseIntegrationTest() {
        @Test
        fun `a session no conversation points at is deleted with its events`() {
            val orphan = createSession(ageDays = 3)
            sessionService.appendMessage(orphan.toString(), UserMessage("never persisted"))
            val attached = createSession(ageDays = 3)
            conversationRepository.save(
                Conversation(title = "Live", sessionId = attached).also { it.createdBy = testUser() },
            )

            job().cleanup()

            assertNull(sessionService.findById(orphan.toString()))
            assertEquals(0, eventCount(orphan))
            assertNotNull(sessionService.findById(attached.toString()))
        }

        @Test
        fun `a session younger than the grace period is left alone`() {
            val inFlight = createSession(ageDays = 0)

            job().cleanup()

            assertNotNull(sessionService.findById(inFlight.toString()))
        }

        @Test
        fun `deleting a conversation drops its session and clears the pointer`() {
            val sessionId = createSession(ageDays = 0)
            val conversation =
                conversationRepository.save(
                    Conversation(title = "To delete", sessionId = sessionId).also { it.createdBy = testUser() },
                )

            commandService.deleteConversation(requireNotNull(conversation.id))

            assertNull(sessionService.findById(sessionId.toString()))
            assertNull(conversationRepository.findById(conversation.id!!).orElseThrow().sessionId)
        }

        private fun job() =
            OrphanSessionCleanupJob(
                conversationRepository = conversationRepository,
                sessionService = sessionService,
                properties = SessionCleanupProperties(orphanGrace = Duration.ofHours(24)),
            )

        private fun testUser() = userRepository.findById(TestAuditorConfig.TEST_USER_ID).orElseThrow()

        private fun createSession(ageDays: Long): UUID {
            val sessionId = UUID.randomUUID()
            sessionService.create(
                CreateSessionRequest
                    .builder()
                    .id(sessionId.toString())
                    .userId(UUID.randomUUID().toString())
                    .build(),
            )
            if (ageDays > 0) {
                // The library stamps created_at itself, so back-date the row to age the session.
                jdbcTemplate.update(
                    "UPDATE ai_session SET created_at = ? WHERE id = ?",
                    LocalDateTime.ofInstant(Instant.now().minus(Duration.ofDays(ageDays)), ZoneId.systemDefault()),
                    sessionId.toString(),
                )
            }
            return sessionId
        }

        private fun eventCount(sessionId: UUID): Int =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_session_event WHERE session_id = ?",
                Int::class.java,
                sessionId.toString(),
            ) ?: 0
    }

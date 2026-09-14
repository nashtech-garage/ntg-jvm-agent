package com.ntgjvmagent.orchestrator.unit.service

import com.ntgjvmagent.orchestrator.component.CurrentUserProvider
import com.ntgjvmagent.orchestrator.entity.User
import com.ntgjvmagent.orchestrator.entity.memory.AgentMemory
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.repository.AgentMemoryRepository
import com.ntgjvmagent.orchestrator.repository.UserRepository
import com.ntgjvmagent.orchestrator.service.AgentMemoryService
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog
import com.ntgjvmagent.orchestrator.tool.MemoryTools
import com.ntgjvmagent.orchestrator.tool.SupportPolicyTool
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.json.JsonMapper
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentMemoryServiceTest {
    private val memoryRepository = mockk<AgentMemoryRepository>()
    private val userRepository = mockk<UserRepository>()
    private val currentUserProvider = mockk<CurrentUserProvider>()
    private val service = AgentMemoryService(memoryRepository, userRepository, currentUserProvider)

    @Test
    fun `memory scope always comes from current user provider and tool exposes no user id`() {
        val authenticatedUserId = UUID.randomUUID()
        val attemptedUserId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val user = user(authenticatedUserId, memoryEnabled = true)
        val savedMemory = slot<AgentMemory>()
        every { currentUserProvider.getUserId() } returns authenticatedUserId
        every { userRepository.findById(authenticatedUserId) } returns Optional.of(user)
        every { memoryRepository.countByUserId(authenticatedUserId) } returns 0
        every {
            memoryRepository.findByUserIdAndAgentIdIsNullAndName(authenticatedUserId, "preferred-language")
        } returns null
        every { memoryRepository.save(capture(savedMemory)) } answers {
            savedMemory.captured.apply { id = UUID.randomUUID() }
        }

        val callback =
            LocalToolCatalog(SupportPolicyTool(), service)
                .getToolCallbacks(agentId, includeMemory = true)
                .single { it.toolDefinition.name() == MemoryTools.CREATE_TOOL_NAME }
        val schema = JsonMapper.builder().build().readTree(callback.toolDefinition.inputSchema())

        assertFalse(schema["properties"].has("userId"))
        assertFalse(schema["properties"].has("agentId"))
        assertFalse(schema["additionalProperties"].asBoolean())
        verify(exactly = 1) { currentUserProvider.getUserId() }
        clearMocks(currentUserProvider, answers = false, recordedCalls = true)
        callback.call(
            """
            {
              "userId": "$attemptedUserId",
              "name": "preferred-language",
              "type": "user",
              "description": "Preferred response language",
              "content": "Vietnamese",
              "agentSpecific": false
            }
            """.trimIndent(),
        )

        assertEquals(authenticatedUserId, savedMemory.captured.userId)
        assertEquals(null, savedMemory.captured.agentId)
        verify(exactly = 0) { currentUserProvider.getUserId() }
        verify(exactly = 0) { userRepository.findById(attemptedUserId) }
    }

    @Test
    fun `create rejects disabled users`() {
        val userId = UUID.randomUUID()
        every { currentUserProvider.getUserId() } returns userId
        every { userRepository.findById(userId) } returns Optional.of(user(userId, memoryEnabled = false))

        val failure =
            assertThrows<BadRequestException> {
                create(content = "small")
            }

        assertTrue(failure.message.orEmpty().contains("disabled"))
        verify(exactly = 0) { memoryRepository.save(any()) }
    }

    @Test
    fun `create enforces per-user count limit`() {
        val userId = UUID.randomUUID()
        every { currentUserProvider.getUserId() } returns userId
        every { userRepository.findById(userId) } returns Optional.of(user(userId, memoryEnabled = true))
        every { memoryRepository.countByUserId(userId) } returns AgentMemoryService.MAX_MEMORIES_PER_USER

        val failure = assertThrows<BadRequestException> { create(content = "small") }

        assertTrue(failure.message.orEmpty().contains("limit"))
        verify(exactly = 0) { memoryRepository.save(any()) }
    }

    @Test
    fun `create enforces content size limit`() {
        val userId = UUID.randomUUID()
        every { currentUserProvider.getUserId() } returns userId
        every { userRepository.findById(userId) } returns Optional.of(user(userId, memoryEnabled = true))

        val failure =
            assertThrows<BadRequestException> {
                create(content = "x".repeat(AgentMemoryService.MAX_CONTENT_LENGTH + 1))
            }

        assertTrue(failure.message.orEmpty().contains(AgentMemoryService.MAX_CONTENT_LENGTH.toString()))
        verify(exactly = 0) { memoryRepository.countByUserId(any()) }
        verify(exactly = 0) { memoryRepository.save(any()) }
    }

    private fun create(content: String) =
        service.create(
            name = "preferred-language",
            type = "user",
            description = "Preferred response language",
            content = content,
            currentAgentId = UUID.randomUUID(),
            agentSpecific = false,
        )

    private fun user(
        id: UUID,
        memoryEnabled: Boolean,
    ) = User(
        id = id,
        username = "user-$id",
        password = "password",
        name = "Test User",
        email = "$id@example.com",
        memoryEnabled = memoryEnabled,
    )
}

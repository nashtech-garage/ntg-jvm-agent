package com.ntgjvmagent.orchestrator.unit.agent

import com.ntgjvmagent.orchestrator.agent.domain.Agent
import com.ntgjvmagent.orchestrator.agent.domain.AgentRepository
import com.ntgjvmagent.orchestrator.agent.service.AgentService
import com.ntgjvmagent.orchestrator.agent.web.dto.AgentRequest
import com.ntgjvmagent.orchestrator.agent.web.mapper.AgentMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgentServiceTest {
    private val repo: AgentRepository = mockk(relaxed = true)
    private val service = AgentService(repo)

    @BeforeEach
    fun setup() {
        clearMocks(repo)
    }

    // --- getAllActive ---
    @Test
    fun `getAllActive should return list of active agents`() {
        val agent = buildAgent()
        every { repo.findAllActive() } returns listOf(agent)

        val result = service.getAllActive()

        assertEquals(1, result.size)
        assertEquals(agent.name, result[0].name)
        verify(exactly = 1) { repo.findAllActive() }
    }

    // --- getById ---
    @Test
    fun `getById should return agent response`() {
        val id = UUID.randomUUID()
        val agent = buildAgent(id)
        every { repo.findByIdNotDeleted(id) } returns Optional.of(agent)

        val result = service.getById(id)

        assertEquals(agent.id, result.id)
        assertEquals(agent.name, result.name)
        verify { repo.findByIdNotDeleted(id) }
    }

    @Test
    fun `getById should throw EntityNotFoundException when not found`() {
        val id = UUID.randomUUID()
        every { repo.findByIdNotDeleted(id) } returns Optional.empty()

        val exception =
            assertThrows<EntityNotFoundException> {
                service.getById(id)
            }

        assertTrue(exception.message!!.contains("Agent not found"))
        verify { repo.findByIdNotDeleted(id) }
    }

    // --- create ---
    @Test
    fun `create should save new agent and return response`() {
        val request = buildRequest()
        val entity = AgentMapper.toEntity(request)
        val saved = entity.copy()
        saved.id = UUID.randomUUID()
        every { repo.save(any()) } returns saved

        val result = service.create(request)

        assertEquals(saved.name, result.name)
        assertNotNull(result.id)
        verify { repo.save(any()) }
    }

    // --- update ---
    @Test
    fun `update should modify existing agent and return updated response`() {
        val id = UUID.randomUUID()
        val existing = buildAgent(id)
        val request = buildRequest(name = "Updated Agent", temperature = 1.0)

        every { repo.findByIdNotDeleted(id) } returns Optional.of(existing)
        every { repo.save(any()) } answers { firstArg() }

        val result = service.update(id, request)

        assertEquals("Updated Agent", result.name)
        assertEquals(1.0, result.temperature)
        verify { repo.findByIdNotDeleted(id) }
        verify { repo.save(existing) }
    }

    @Test
    fun `update should throw EntityNotFoundException when agent missing`() {
        val id = UUID.randomUUID()
        every { repo.findByIdNotDeleted(id) } returns Optional.empty()

        val exception =
            assertThrows<EntityNotFoundException> {
                service.update(id, buildRequest())
            }

        assertTrue(exception.message!!.contains("Agent not found"))
        verify { repo.findByIdNotDeleted(id) }
    }

    // --- softDelete ---
    @Test
    fun `softDelete should set deletedAt timestamp`() {
        val id = UUID.randomUUID()
        val existing = buildAgent(id)

        every { repo.findByIdNotDeleted(id) } returns Optional.of(existing)
        every { repo.save(any()) } returns existing

        service.softDelete(id)

        assertNotNull(existing.deletedAt)
        verify { repo.findByIdNotDeleted(id) }
        verify { repo.save(existing) }
    }

    @Test
    fun `softDelete should throw EntityNotFoundException when not found`() {
        val id = UUID.randomUUID()
        every { repo.findByIdNotDeleted(id) } returns Optional.empty()

        val exception =
            assertThrows<EntityNotFoundException> {
                service.softDelete(id)
            }

        assertTrue(exception.message!!.contains("Agent not found"))
        verify { repo.findByIdNotDeleted(id) }
    }

    // --- helpers ---

    private fun buildAgent(id: UUID = UUID.randomUUID()) =
        Agent(
            name = "Test Agent",
            model = "gpt-4o-mini",
            description = "desc",
            temperature = BigDecimal("0.7"),
            maxTokens = 2048,
            topP = BigDecimal("1.0"),
            frequencyPenalty = BigDecimal("0.0"),
            presencePenalty = BigDecimal("0.0"),
            active = true,
        ).apply {
            this.id = id
            this.createdAt = ZonedDateTime.now()
        }

    private fun buildRequest(
        name: String = "Test Agent",
        temperature: Double = 0.7,
    ) = AgentRequest(
        name = name,
        model = "gpt-4o-mini",
        description = "desc",
        temperature = temperature,
        maxTokens = 2048,
        topP = 1.0,
        frequencyPenalty = 0.0,
        presencePenalty = 0.0,
        active = true,
        provider = "openai",
        settings = mapOf("max_retries" to 3),
    )
}

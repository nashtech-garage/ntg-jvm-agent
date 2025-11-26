package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.AgentRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.service.AgentService
import com.ntgjvmagent.orchestrator.support.SoftDeleteAssertions.assertSoftDeleted
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class AgentServiceIT
    @Autowired
    constructor(
        private val service: AgentService,
        private val repo: AgentRepository,
        private val entityManager: EntityManager,
    ) : BaseIntegrationTest() {
        private fun buildRequest(
            name: String = "Agent A",
            active: Boolean = true,
        ) = AgentRequestDto(
            name = name,
            model = "gpt-4o-mini",
            description = "integration test agent",
            temperature = 0.7,
            maxTokens = 2048,
            topP = 1.0,
            frequencyPenalty = 0.0,
            presencePenalty = 0.0,
            active = active,
            provider = "openai",
            settings = mapOf("max_retries" to 3),
            baseUrl = "https://models.github.ai/inference",
            apiKey = "fake-github-token",
            chatCompletionsPath = "/v1/chat/completions",
            embeddingsPath = "/embeddings",
            embeddingModel = "openai/text-embedding-3-small",
            dimension = 1536,
        )

        // ---------------------------
        // CREATE
        // ---------------------------
        @Test
        fun `should create agent successfully`() {
            val request = buildRequest("AgentCreate")
            val created = service.create(request)

            assertNotNull(created.id)
            assertEquals("AgentCreate", created.name)

            val saved = repo.findById(created.id).orElseThrow()
            assertEquals("gpt-4o-mini", saved.model)
            assertTrue(saved.active)
            assertEquals("openai", saved.provider)
        }

        // ---------------------------
        // GET BY ID
        // ---------------------------
        @Test
        fun `should fetch agent by id`() {
            val saved =
                repo
                    .save(
                        Agent(
                            name = "AgentGet",
                            model = "gpt-4o-mini",
                            baseUrl = "https://models.github.ai/inference",
                            apiKey = "fake-github-token",
                            chatCompletionsPath = "/v1/chat/completions",
                            embeddingsPath = "/embeddings",
                            embeddingModel = "openai/text-embedding-3-small",
                        ),
                    )
            val found = service.getById(saved.id!!)

            assertEquals(saved.id, found.id)
            assertEquals("AgentGet", found.name)
        }

        @Test
        fun `should throw EntityNotFoundException when agent missing`() {
            val missingId = UUID.randomUUID()

            val exception =
                assertThrows(EntityNotFoundException::class.java) {
                    service.getById(missingId)
                }

            assertTrue(exception.message!!.contains("Agent not found"))
        }

        // ---------------------------
        // GET ALL ACTIVE
        // ---------------------------
        @Test
        fun `should return only active agents`() {
            repo.save(
                Agent(
                    name = "ActiveA",
                    model = "gpt-4o-mini",
                    baseUrl = "https://models.github.ai/inference",
                    apiKey = "fake-github-token",
                    chatCompletionsPath = "/v1/chat/completions",
                    embeddingsPath = "/embeddings",
                    embeddingModel = "openai/text-embedding-3-small",
                ).apply { this.active = true },
            )
            repo.save(
                Agent(
                    name = "InactiveB",
                    model = "gpt-4o-mini",
                    baseUrl = "https://models.github.ai/inference",
                    apiKey = "fake-github-token",
                    chatCompletionsPath = "/v1/chat/completions",
                    embeddingsPath = "/embeddings",
                    embeddingModel = "openai/text-embedding-3-small",
                ).apply { this.active = false },
            )

            val activeAgents = service.getAllActive()

            assertTrue(activeAgents.all { it.active })
            assertTrue(activeAgents.any { it.name == "ActiveA" })
            assertTrue(activeAgents.none { it.name == "InactiveB" })
        }

        // ---------------------------
        // UPDATE
        // ---------------------------
        @Test
        fun `should update agent successfully`() {
            val saved =
                repo
                    .save(
                        Agent(
                            name = "BeforeUpdate",
                            model = "gpt-4o-mini",
                            baseUrl = "https://models.github.ai/inference",
                            apiKey = "fake-github-token",
                            chatCompletionsPath = "/v1/chat/completions",
                            embeddingsPath = "/embeddings",
                            embeddingModel = "openai/text-embedding-3-small",
                        ),
                    )

            val updateReq =
                buildRequest(
                    name = "AfterUpdate",
                    active = false,
                ).copy(description = "Updated agent")

            val updated = service.update(saved.id!!, updateReq)

            assertEquals("AfterUpdate", updated.name)
            assertFalse(updated.active)
            assertEquals("Updated agent", updated.description)
        }

        @Test
        fun `should throw EntityNotFoundException when updating missing agent`() {
            val missingId = UUID.randomUUID()
            val request = buildRequest("MissingUpdate")

            val ex =
                assertThrows(EntityNotFoundException::class.java) {
                    service.update(missingId, request)
                }

            assertTrue(ex.message!!.contains("Agent not found"))
        }

        // ---------------------------
        // SOFT DELETE
        // ---------------------------
        @Test
        fun `should soft delete agent`() {
            val saved =
                repo.save(
                    Agent(
                        name = "SoftDelete",
                        model = "gpt-4o-mini",
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        embeddingsPath = "/embeddings",
                        embeddingModel = "openai/text-embedding-3-small",
                    ),
                )

            service.softDelete(saved.id!!)

            assertSoftDeleted(entityManager, Agent::class.java, saved.id!!)
        }

        @Test
        fun `should throw EntityNotFoundException when deleting missing agent`() {
            val id = UUID.randomUUID()

            val ex =
                assertThrows(EntityNotFoundException::class.java) {
                    service.softDelete(id)
                }

            assertTrue(ex.message!!.contains("Agent not found"))
        }
    }

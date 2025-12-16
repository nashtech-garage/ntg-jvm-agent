package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.request.InlineKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeService
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Transactional
class AgentKnowledgeServiceIT
    @Autowired
    constructor(
        private val service: AgentKnowledgeService,
        private val repo: AgentKnowledgeRepository,
        private val agentRepo: AgentRepository,
    ) : BaseIntegrationTest() {
        private lateinit var agent: Agent
        private lateinit var knowledge: AgentKnowledge

        @BeforeEach
        fun setUp() {
            repo.deleteAll()
            agentRepo.deleteAll()
            agentRepo.flush()
            repo.flush()

            agent =
                agentRepo.save(
                    Agent(
                        name = "Test Agent",
                        provider = ProviderType.OPENAI,
                        baseUrl = "https://models.github.ai/inference",
                        apiKey = "fake-github-token",
                        chatCompletionsPath = "/v1/chat/completions",
                        model = "gpt-4",
                        embeddingModel = "openai/text-embedding-3-small",
                        embeddingsPath = "/embeddings",
                    ),
                )

            knowledge =
                repo.save(
                    AgentKnowledge(
                        agent = agent,
                        name = "Inline Content",
                        sourceType = KnowledgeSourceType.INLINE,
                        sourceUri = null,
                        metadata = mapOf("content" to "Some stealth instructions"),
                    ).apply { active = true },
                )
        }

        @Test
        fun `getByAgent should return only knowledge for agent`() {
            val result = service.getByAgent(agent.id!!)
            assertEquals(1, result.size)
            assertEquals(knowledge.id, result.first().id)
        }

        @Test
        fun `getOneForAgent should return knowledge by id for agent`() {
            val result = service.getOneForAgent(agent.id!!, knowledge.id!!)
            assertEquals(knowledge.id, result.id)
            assertEquals(knowledge.name, result.name)
        }

        @Test
        fun `create should save new INLINE knowledge for agent`() {
            val request =
                InlineKnowledgeRequestDto(
                    inlineContent = "Hand-to-hand combat guide",
                )

            val result = service.create(agent.id!!, request)

            val saved = repo.findById(result.id).get()

            assertEquals(KnowledgeSourceType.INLINE, saved.sourceType)
            assertEquals("Hand-to-hand combat guide", saved.metadata["content"])
            assertTrue(saved.active)
            assertNotNull(saved.name) // auto-generated
            assertTrue(saved.name.isNotBlank())
        }

        @Test
        fun `update should modify metadata and trigger re-ingestion`() {
            val updateRequest =
                InlineKnowledgeRequestDto(
                    inlineContent = "Advanced stealth techniques",
                )

            val result = service.update(agent.id!!, knowledge.id!!, updateRequest)

            val updated = repo.findById(knowledge.id!!).get()

            // Name must NOT change
            assertEquals(knowledge.name, updated.name)

            // Metadata changed
            assertEquals("Advanced stealth techniques", updated.metadata["content"])
            assertEquals(KnowledgeSourceType.INLINE, updated.sourceType)

            // Re-ingestion state
            assertEquals(KnowledgeStatus.PENDING, updated.status)
            assertTrue(updated.active)
        }

        @Test
        fun `softDelete should mark knowledge as deleted for agent`() {
            service.softDelete(agent.id!!, knowledge.id!!)

            val deleted = repo.findById(knowledge.id!!).get()

            assertNotNull(deleted.deletedAt)
            assertFalse(deleted.active)
        }

        @Test
        fun `softDelete should throw exception for non-existing knowledge for agent`() {
            val randomId = UUID.randomUUID()

            val exception =
                assertThrows<EntityNotFoundException> {
                    service.softDelete(agent.id!!, randomId)
                }

            assertTrue(
                exception.message!!.contains(
                    "Knowledge $randomId not found for agent ${agent.id}",
                ),
            )
        }
    }

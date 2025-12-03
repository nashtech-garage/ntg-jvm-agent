package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.request.InlineKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeService
import jakarta.persistence.EntityNotFoundException
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
                        provider = "OpenAI",
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
                        name = "Stealth Training",
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
        fun `create should save new knowledge for agent`() {
            val request =
                InlineKnowledgeRequestDto(
                    name = "Combat Training",
                    inlineContent = "Hand-to-hand combat guide",
                )

            val result = service.create(agent.id!!, request)

            assertEquals(request.name, result.name)
            assertTrue(repo.existsById(result.id))
            assertEquals("Hand-to-hand combat guide", result.metadata["content"])
            assertEquals(KnowledgeSourceType.INLINE, result.sourceType)
            assertEquals(true, result.active)
        }

        @Test
        fun `update should modify existing knowledge for agent`() {
            val updateRequest =
                InlineKnowledgeRequestDto(
                    name = "Stealth Mastery",
                    inlineContent = "Advanced stealth techniques",
                )

            val result = service.update(agent.id!!, knowledge.id!!, updateRequest)

            assertEquals("Stealth Mastery", result.name)
            assertEquals("Advanced stealth techniques", result.metadata["content"])
            assertEquals(KnowledgeSourceType.INLINE, result.sourceType)
            assertEquals(true, result.active) // update does NOT toggle active
        }

        @Test
        fun `softDelete should mark knowledge as deleted for agent`() {
            service.softDelete(agent.id!!, knowledge.id!!)

            val deleted = repo.findById(knowledge.id!!).get()
            assertTrue(deleted.deletedAt != null)
            assertEquals(false, deleted.active)
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

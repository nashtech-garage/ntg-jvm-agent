package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
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
    ) : BaseIntegrationTest() {
        private lateinit var knowledge: AgentKnowledge

        @BeforeEach
        fun setUp() {
            repo.deleteAll()
            repo.flush()
            knowledge =
                repo.save(
                    AgentKnowledge(
                        name = "Stealth Training",
                        sourceType = "manual",
                        sourceUri = "http://example.com/stealth",
                        metadata = emptyMap(),
                        embeddingModel = "text-embedding-3-small",
                    ).apply { active = true },
                )
        }

        @Test
        fun `getAllActive should return only active knowledge`() {
            val result = service.getAllActive()
            assertEquals(1, result.size)
            assertEquals(knowledge.id, result.first().id)
        }

        @Test
        fun `getById should return knowledge by id`() {
            val result = service.getById(knowledge.id!!)
            assertEquals(knowledge.id, result.id)
            assertEquals(knowledge.name, result.name)
        }

        @Test
        fun `getById should throw EntityNotFoundException for non-existing id`() {
            val randomId = UUID.randomUUID()
            val exception =
                assertThrows<EntityNotFoundException> {
                    service.getById(randomId)
                }
            assertTrue(exception.message!!.contains("Knowledge not found"))
        }

        @Test
        fun `create should save new knowledge`() {
            val request =
                AgentKnowledgeRequestDto(
                    name = "Combat Training",
                    sourceType = "manual",
                    sourceUri = "http://example.com/combat",
                    metadata = emptyMap(),
                    embeddingModel = "text-embedding-3-small",
                    active = true,
                )
            val result = service.create(request)
            assertEquals(request.name, result.name)
            assertTrue(repo.existsById(result.id))
        }

        @Test
        fun `update should modify existing knowledge`() {
            val updateRequest =
                AgentKnowledgeRequestDto(
                    name = "Stealth Mastery",
                    sourceType = "manual",
                    sourceUri = "http://example.com/stealth-master",
                    metadata = mapOf("level" to "advanced"),
                    embeddingModel = "text-embedding-3-large",
                    active = false,
                )

            val result = service.update(knowledge.id!!, updateRequest)
            assertEquals("Stealth Mastery", result.name)
            assertEquals("text-embedding-3-large", result.embeddingModel)
            assertEquals(false, result.active)
        }

        @Test
        fun `softDelete should mark knowledge as deleted`() {
            service.softDelete(knowledge.id!!)
            val deleted = repo.findById(knowledge.id!!).get()
            assertTrue(deleted.deletedAt != null)
        }

        @Test
        fun `softDelete should throw exception for non-existing knowledge`() {
            val randomId = UUID.randomUUID()
            val exception =
                assertThrows<EntityNotFoundException> {
                    service.softDelete(randomId)
                }
            assertTrue(exception.message!!.contains("Knowledge not found"))
        }
    }

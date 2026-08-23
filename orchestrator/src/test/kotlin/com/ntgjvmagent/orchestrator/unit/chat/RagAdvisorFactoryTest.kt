package com.ntgjvmagent.orchestrator.unit.chat

import com.ntgjvmagent.orchestrator.advisor.RagAdvisorFactory
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.utils.Constant
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter
import reactor.core.publisher.Flux
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RagAdvisorFactoryTest {
    private val agentId = UUID.randomUUID()

    @Test
    fun `RAG stays inactive when the agent has no attached knowledge`() {
        val repository = mockk<AgentKnowledgeRepository>()
        val vectorStoreService = mockk<VectorStoreService>(relaxed = true)
        every { repository.findAllByAgentIdAndActiveTrue(agentId) } returns emptyList()

        val advisor = RagAdvisorFactory(vectorStoreService, repository).create(agentId)

        assertNull(advisor)
        verify(exactly = 0) { vectorStoreService.getVectorStore() }
    }

    @Test
    fun `RAG uses the existing store with top five and an attached knowledge filter`() {
        val knowledgeId = UUID.randomUUID()
        val knowledge = AgentKnowledge.stub().apply { id = knowledgeId }
        val repository = mockk<AgentKnowledgeRepository>()
        every { repository.findAllByAgentIdAndActiveTrue(agentId) } returns listOf(knowledge)

        val requestSlot = slot<SearchRequest>()
        val vectorStore = mockk<VectorStore>()
        every { vectorStore.similaritySearch(capture(requestSlot)) } returns
            listOf(
                Document(
                    "Employees may carry over up to five vacation days.",
                    mapOf("knowledgeId" to knowledgeId.toString()),
                ),
            )
        val vectorStoreService = mockk<VectorStoreService>()
        every { vectorStoreService.getVectorStore() } returns vectorStore

        val advisor = RagAdvisorFactory(vectorStoreService, repository).create(agentId)!!
        val answer =
            ChatClient
                .builder(FixedChatModel("Employees may carry over five vacation days."))
                .defaultAdvisors(advisor)
                .build()
                .prompt()
                .user("How many vacation days can I carry over?")
                .call()
                .content()

        assertEquals("Employees may carry over five vacation days.", answer)
        assertEquals(Constant.TOP_K, requestSlot.captured.topK)
        assertEquals(
            Filter.Expression(
                Filter.ExpressionType.IN,
                Filter.Key("knowledgeId"),
                Filter.Value(listOf(knowledgeId.toString())),
            ),
            requestSlot.captured.filterExpression,
        )
    }

    private class FixedChatModel(
        private val responseText: String,
    ) : ChatModel {
        override fun call(prompt: Prompt): ChatResponse =
            ChatResponse(listOf(Generation(AssistantMessage(responseText))))

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(call(prompt))
    }
}

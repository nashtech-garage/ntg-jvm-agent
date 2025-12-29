package com.ntgjvmagent.orchestrator.advisor

import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RagAdvisorFactory(
    private val vectorStoreService: VectorStoreService,
    private val agentKnowledgeRepo: AgentKnowledgeRepository,
) {
    fun createRagAdvisor(agentId: UUID): CallAdvisor? = buildRagAdvisorForAgent(agentId)

    fun createChatMemoryAdvisor(
        agentId: UUID,
        conversationId: UUID?,
    ): CallAdvisor? = buildChatMemoryAdvisor(agentId, conversationId)

    private fun buildRagAdvisorForAgent(agentId: UUID): CallAdvisor? {
        val knowledgeIds =
            agentKnowledgeRepo
                .findAllByAgentIdAndActiveTrue(agentId)
                .map { it.id.toString() }

        if (knowledgeIds.isEmpty()) return null

        val filterExpression =
            Filter.Expression(
                Filter.ExpressionType.IN,
                Filter.Key("knowledgeId"),
                Filter.Value(knowledgeIds),
            )

        val documentRetriever =
            VectorStoreDocumentRetriever
                .builder()
                .vectorStore(vectorStoreService.getVectorStore(agentId))
                .topK(Constant.TOP_K)
                .filterExpression(filterExpression)
                .build()

        val queryAugmenter =
            ContextualQueryAugmenter
                .builder()
                .allowEmptyContext(true)
                .build()

        return RetrievalAugmentationAdvisor
            .builder()
            .documentRetriever(documentRetriever)
            .queryAugmenter(queryAugmenter)
            .order(Constant.RAG_ADVISOR_ORDER)
            .build()
    }

    private fun buildChatMemoryAdvisor(
        agentId: UUID,
        conversationId: UUID?,
    ): CallAdvisor? {
        if (conversationId == null) return null

        val filter =
            Filter.Expression(
                Filter.ExpressionType.AND,
                Filter.Expression(
                    Filter.ExpressionType.EQ,
                    Filter.Key("agentId"),
                    Filter.Value(agentId.toString()),
                ),
                Filter.Expression(
                    Filter.ExpressionType.AND,
                    Filter.Expression(
                        Filter.ExpressionType.EQ,
                        Filter.Key("conversationId"),
                        Filter.Value(conversationId.toString()),
                    ),
                    Filter.Expression(
                        Filter.ExpressionType.EQ,
                        Filter.Key("type"),
                        Filter.Value("chat_message"),
                    ),
                ),
            )

        val retriever =
            VectorStoreDocumentRetriever
                .builder()
                .vectorStore(vectorStoreService.getVectorStore(agentId))
                .topK(Constant.CHAT_MEMORY_TOP_K)
                .filterExpression(filter)
                .build()

        val queryAugmenter =
            ContextualQueryAugmenter
                .builder()
                .allowEmptyContext(true)
                .build()

        return RetrievalAugmentationAdvisor
            .builder()
            .documentRetriever(retriever)
            .queryAugmenter(queryAugmenter)
            .order(Constant.CHAT_MEMORY_ADVISOR_ORDER)
            .build()
    }
}

package com.ntgjvmagent.orchestrator.config

import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RagConfig(
    private val vectorStore: VectorStore,
) {
    @Bean
    fun retriever(): VectorStoreDocumentRetriever =
        VectorStoreDocumentRetriever
            .builder()
            .vectorStore(vectorStore)
            .topK(Constant.TOP_K)
            .build()

    @Bean
    fun qaAdvisor(): QuestionAnswerAdvisor =
        QuestionAnswerAdvisor
            .builder(vectorStore)
            .searchRequest(
                SearchRequest
                    .builder()
                    .topK(Constant.TOP_K)
                    .build(),
            ).build()
}

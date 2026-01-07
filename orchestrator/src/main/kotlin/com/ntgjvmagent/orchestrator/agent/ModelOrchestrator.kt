package com.ntgjvmagent.orchestrator.agent

import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.embedding.runtime.ReactiveEmbeddingModel
import com.ntgjvmagent.orchestrator.embedding.runtime.adapter.SpringAiEmbeddingModelAdapter
import com.ntgjvmagent.orchestrator.mapper.AgentMapper
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.utils.Quadruple
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service
import org.springframework.ai.embedding.EmbeddingModel as SpringEmbeddingModel

@Service
class ModelOrchestrator(
    private val chatModelOrchestrator: ChatModelOrchestrator,
    private val embeddingModelOrchestrator: EmbeddingModelOrchestrator,
) {
    fun create(
        agent: com.ntgjvmagent.orchestrator.entity.agent.Agent,
    ): Quadruple<ChatModel, ReactiveEmbeddingModel, SpringEmbeddingModel, AgentResponseDto> {
        val chatModel = chatModelOrchestrator.createChatModel(ChatModelConfig.fromAgent(agent))

        val springEmbeddingModel =
            embeddingModelOrchestrator.createEmbeddingModel(EmbeddingModelConfig.fromAgent(agent))

        val wrappedEmbeddingModel = SpringAiEmbeddingModelAdapter(springEmbeddingModel)
        val agentConfig = AgentMapper.toResponse(agent)

        return Quadruple(chatModel, wrappedEmbeddingModel, springEmbeddingModel, agentConfig)
    }
}

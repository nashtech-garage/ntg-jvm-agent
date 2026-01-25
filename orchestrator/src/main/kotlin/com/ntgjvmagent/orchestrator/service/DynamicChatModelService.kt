package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.chat.ChatModelOrchestrator
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.mapper.AgentMapper
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.ai.chat.model.ChatModel
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class DynamicChatModelService(
    private val chatModelOrchestrator: ChatModelOrchestrator,
    private val agentRepo: AgentRepository,
) {
    private val cache =
        ConcurrentHashMap<UUID, Pair<ChatModel, AgentResponseDto>>()

    fun getChatModel(agentId: UUID): ChatModel = cache.computeIfAbsent(agentId) { create(it) }.first

    fun getAgentConfig(agentId: UUID): AgentResponseDto = cache.computeIfAbsent(agentId) { create(it) }.second

    fun reload(agentId: UUID) {
        cache.compute(agentId) { _, _ -> create(agentId) }
    }

    private fun create(agentId: UUID): Pair<ChatModel, AgentResponseDto> {
        val agent =
            agentRepo.findByIdOrNull(agentId)
                ?: throw EntityNotFoundException("Agent $agentId not found")

        val chatModel =
            chatModelOrchestrator.createChatModel(
                ChatModelConfig.fromAgent(agent),
            )

        return chatModel to AgentMapper.toResponse(agent)
    }
}

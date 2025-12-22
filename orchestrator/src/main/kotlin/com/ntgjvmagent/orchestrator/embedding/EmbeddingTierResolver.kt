package com.ntgjvmagent.orchestrator.embedding

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingTierConfig
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EmbeddingTierResolver(
    private val agentRepo: AgentRepository,
    private val tierCache: EmbeddingTierConfigCacheService,
) {
    private companion object {
        private const val DEFAULT_TIER = "free"
    }

    fun resolve(agentId: UUID): Pair<String, EmbeddingTierConfig> {
        val agent = agentRepo.findById(agentId).orElseThrow()
        val tier =
            (agent.settings?.get("tier") ?: DEFAULT_TIER)
                .toString()
                .lowercase()

        return tier to tierCache[tier]
    }
}

package com.ntgjvmagent.orchestrator.embedding

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.EmbeddingTierConfig
import com.ntgjvmagent.orchestrator.repository.EmbeddingTierConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class EmbeddingTierConfigCacheService(
    private val repo: EmbeddingTierConfigRepository,
) {
    private val cache = ConcurrentHashMap<String, EmbeddingTierConfig>()
    private val log = LoggerFactory.getLogger(EmbeddingTierConfigCacheService::class.java)

    init {
        refreshAll() // initial load
    }

    @Scheduled(fixedDelay = 10_000) // every 10s, refresh configs
    fun refreshAll() {
        repo.findAll().forEach { config ->
            cache[config.tierName.lowercase()] = config
        }
        log.debug("Loaded embedding tier configs: {}", cache.keys)
    }

    operator fun get(tier: String): EmbeddingTierConfig =
        cache[tier.lowercase()] ?: cache["free"]
            ?: error("No tier config for '$tier' and no 'free' default")
}

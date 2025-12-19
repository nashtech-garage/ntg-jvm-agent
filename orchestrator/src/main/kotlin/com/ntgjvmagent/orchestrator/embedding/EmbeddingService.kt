package com.ntgjvmagent.orchestrator.embedding

import com.ntgjvmagent.orchestrator.service.DynamicModelService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.UUID

@Service
class EmbeddingService(
    private val dynamicModelService: DynamicModelService,
    private val tierResolver: EmbeddingTierResolver,
    private val rateLimiter: EmbeddingRateLimiter,
    private val tokenTracker: EmbeddingAccountingFacade,
) {
    private companion object {
        private val BLOCKING_TIMEOUT = Duration.ofSeconds(30)
    }

    fun embed(
        agentId: UUID,
        text: String,
        correlationId: String,
    ): FloatArray =
        embedReactive(agentId, text, correlationId)
            .publishOn(Schedulers.boundedElastic())
            .block(BLOCKING_TIMEOUT)
            ?: error("Embedding timed out for agent $agentId")

    fun embedBatch(
        agentId: UUID,
        texts: List<String>,
        correlationId: String,
    ): List<FloatArray> =
        embedBatchReactive(agentId, texts, correlationId)
            .publishOn(Schedulers.boundedElastic())
            .block(BLOCKING_TIMEOUT)
            ?: error("Batch embedding timed out for agent $agentId")

    fun embedReactive(
        agentId: UUID,
        text: String,
        correlationId: String,
    ): Mono<FloatArray> = execute(agentId, text, correlationId) { model -> model.embedReactive(text) }

    fun embedBatchReactive(
        agentId: UUID,
        texts: List<String>,
        correlationId: String,
    ): Mono<List<FloatArray>> = execute(agentId, texts, correlationId) { model -> model.embedBatchReactive(texts) }

    private fun <T : Any> execute(
        agentId: UUID,
        input: Any,
        correlationId: String,
        block: (ReactiveEmbeddingModel) -> Mono<T>,
    ): Mono<T> {
        val (tier, cfg) = tierResolver.resolve(agentId)
        val model = dynamicModelService.getEmbeddingModel(agentId)
        val agentConfig = dynamicModelService.getAgentConfig(agentId)

        return rateLimiter
            .apply(agentId, tier, cfg, block(model))
            .doOnSuccess { tokenTracker.record(agentId, agentConfig.model, input, correlationId) }
    }
}

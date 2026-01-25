package com.ntgjvmagent.orchestrator.embedding.runtime

import com.ntgjvmagent.orchestrator.config.EmbeddingProperties
import com.ntgjvmagent.orchestrator.embedding.accounting.EmbeddingAccountingFacade
import com.ntgjvmagent.orchestrator.embedding.accounting.EmbeddingRateLimiter
import com.ntgjvmagent.orchestrator.embedding.accounting.EmbeddingTierResolver
import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.embedding.orchestrator.EmbeddingModelOrchestrator
import com.ntgjvmagent.orchestrator.embedding.runtime.adapter.SpringAiEmbeddingModelAdapter
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.UUID

@Service
class EmbeddingService(
    private val embeddingModelOrchestrator: EmbeddingModelOrchestrator,
    private val embeddingProps: EmbeddingProperties,
    private val tierResolver: EmbeddingTierResolver,
    private val rateLimiter: EmbeddingRateLimiter,
    private val tokenTracker: EmbeddingAccountingFacade,
) {
    companion object {
        private val BLOCKING_TIMEOUT = Duration.ofSeconds(30)
    }

    @Volatile
    private var rawModel: EmbeddingModel? = null

    @Volatile
    private var reactiveModel: ReactiveEmbeddingModel? = null

    /* ------------------------------------------------------------------
     * Model access (GLOBAL)
     * ------------------------------------------------------------------ */

    private fun getRawModel(): EmbeddingModel =
        rawModel ?: synchronized(this) {
            rawModel ?: buildRawModel().also { rawModel = it }
        }

    private fun getReactiveModel(): ReactiveEmbeddingModel =
        reactiveModel ?: synchronized(this) {
            reactiveModel ?: SpringAiEmbeddingModelAdapter(getRawModel())
                .also { reactiveModel = it }
        }

    private fun buildRawModel(): EmbeddingModel {
        val config = EmbeddingModelConfig.fromEmbeddingProperties(embeddingProps)
        return embeddingModelOrchestrator.createEmbeddingModel(config)
    }

    private fun modelName(): String = embeddingProps.model

    fun embeddingModel(): EmbeddingModel = getRawModel()

    /* ------------------------------------------------------------------
     * Blocking APIs
     * ------------------------------------------------------------------ */

    fun embed(
        agentId: UUID,
        text: String,
        correlationId: String,
    ): FloatArray =
        embedReactive(agentId, text, correlationId)
            .publishOn(Schedulers.boundedElastic())
            .block(BLOCKING_TIMEOUT)
            ?: error("Embedding timed out")

    fun embedBatch(
        agentId: UUID,
        texts: List<String>,
        correlationId: String,
    ): List<FloatArray> =
        embedBatchReactive(agentId, texts, correlationId)
            .publishOn(Schedulers.boundedElastic())
            .block(BLOCKING_TIMEOUT)
            ?: error("Batch embedding timed out")

    /* ------------------------------------------------------------------
     * Reactive APIs
     * ------------------------------------------------------------------ */

    fun embedReactive(
        agentId: UUID,
        text: String,
        correlationId: String,
    ): Mono<FloatArray> =
        execute(agentId, text, correlationId) { model ->
            model.embedReactive(text)
        }

    fun embedBatchReactive(
        agentId: UUID,
        texts: List<String>,
        correlationId: String,
    ): Mono<List<FloatArray>> =
        execute(agentId, texts, correlationId) { model ->
            model.embedBatchReactive(texts)
        }

    /* ------------------------------------------------------------------
     * Core execution pipeline
     * ------------------------------------------------------------------ */

    private fun <T : Any> execute(
        agentId: UUID,
        input: Any,
        correlationId: String,
        block: (ReactiveEmbeddingModel) -> Mono<T>,
    ): Mono<T> {
        val (tier, cfg) = tierResolver.resolve(agentId)
        val model = getReactiveModel()

        return rateLimiter
            .apply(agentId, tier, cfg, block(model))
            .doFinally { signalType ->
                when (signalType) {
                    SignalType.ON_COMPLETE,
                    SignalType.ON_ERROR,
                    SignalType.CANCEL,
                    -> {
                        tokenTracker.record(
                            agentId = agentId,
                            model = modelName(),
                            input = input,
                            correlationId =
                                if (signalType == SignalType.ON_COMPLETE) {
                                    correlationId
                                } else {
                                    "$correlationId:attempt"
                                },
                        )
                    }

                    else -> {
                        Unit
                    }
                }
            }
    }
}

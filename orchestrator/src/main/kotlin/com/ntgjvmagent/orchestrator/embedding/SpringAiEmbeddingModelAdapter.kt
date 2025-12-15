package com.ntgjvmagent.orchestrator.embedding

import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import org.springframework.ai.embedding.EmbeddingModel as SpringEmbeddingModel

/**
 * Adapter that turns Spring AI's blocking EmbeddingModel into a non-blocking
 * reactive EmbeddingModel suitable for our resilient retry pipeline.
 *
 * All blocking calls are offloaded to boundedElastic() so the scheduler
 * and request threads are never blocked.
 */
class SpringAiEmbeddingModelAdapter(
    private val delegate: SpringEmbeddingModel,
) : ReactiveEmbeddingModel {
    override fun embedReactive(text: String): Mono<FloatArray> =
        Mono
            .fromCallable {
                delegate.embed(text)
            }.subscribeOn(Schedulers.boundedElastic())

    override fun embedBatchReactive(texts: List<String>): Mono<List<FloatArray>> =
        Mono
            .fromCallable {
                delegate.embed(texts)
            }.subscribeOn(Schedulers.boundedElastic())

    /**
     * Expose the underlying Spring model for PgVectorStore usage.
     */
    fun rawSpringModel(): SpringEmbeddingModel = delegate
}

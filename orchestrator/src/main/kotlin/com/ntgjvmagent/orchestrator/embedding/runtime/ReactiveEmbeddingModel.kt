package com.ntgjvmagent.orchestrator.embedding.runtime

import reactor.core.publisher.Mono

/**
 * Reactive abstraction for embedding operations.
 * This is the model consumed by EmbeddingService.
 */
interface ReactiveEmbeddingModel {
    fun embedReactive(text: String): Mono<FloatArray>

    fun embedBatchReactive(texts: List<String>): Mono<List<FloatArray>>
}

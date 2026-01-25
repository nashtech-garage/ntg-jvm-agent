package com.ntgjvmagent.orchestrator.embedding.vectorstore

import com.ntgjvmagent.orchestrator.config.EmbeddingProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component

@Component
class VectorStoreInitializer(
    private val backend: VectorStoreBackend,
    private val embeddingProps: EmbeddingProperties,
) : SmartInitializingSingleton {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun afterSingletonsInstantiated() {
        log.info("Initializing vector store backend: {}", backend.type)

        backend.initialize()
        backend.verifyDimension(embeddingProps.dimension)

        log.info("Vector store backend '{}' ready", backend.type)
    }
}

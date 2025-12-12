package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "embedding.worker")
data class EmbeddingWorkerProperties(
    var minConcurrency: Int = 1,
    var maxConcurrency: Int = 16,
    var stepUp: Int = 1,
    var stepDown: Int = 1,
    var pollIntervalMs: Long = 500,
)

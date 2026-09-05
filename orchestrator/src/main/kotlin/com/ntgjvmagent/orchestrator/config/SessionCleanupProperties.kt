package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "session.cleanup")
data class SessionCleanupProperties(
    val enabled: Boolean = true,
    /** How long a session may exist without a conversation before it counts as abandoned. */
    val orphanGrace: Duration = Duration.ofHours(DEFAULT_ORPHAN_GRACE_HOURS),
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    val maxBatches: Int = DEFAULT_MAX_BATCHES,
) {
    init {
        require(!orphanGrace.isNegative && !orphanGrace.isZero) { "orphanGrace must be positive" }
        require(batchSize > 0) { "batchSize must be greater than 0" }
        require(maxBatches > 0) { "maxBatches must be greater than 0" }
    }

    companion object {
        private const val DEFAULT_ORPHAN_GRACE_HOURS = 24L
        private const val DEFAULT_BATCH_SIZE = 500
        private const val DEFAULT_MAX_BATCHES = 20
    }
}

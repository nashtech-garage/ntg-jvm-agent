package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "pending-question")
data class PendingQuestionProperties(
    val ttl: Duration = Duration.ofHours(DEFAULT_TTL_HOURS),
) {
    init {
        require(!ttl.isNegative && !ttl.isZero) { "ttl must be positive" }
    }

    private companion object {
        const val DEFAULT_TTL_HOURS = 24L
    }
}

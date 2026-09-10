package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Lifetime of the per-session tool search index.
 *
 * The two durations are what keeps the index consistent, so they are validated against each
 * other: a session is re-indexed once its entry is [maxIndexAge] old, while its documents are
 * only swept once they are [documentTtl] old. Both ages are measured from the same moment -- the
 * indexing itself -- so a live in-memory entry is always younger than documents old enough to be
 * swept, and the sweep can never pull the index out from under a session still being served.
 */
@ConfigurationProperties(prefix = "tool-search.index")
data class ToolSearchIndexProperties(
    val cleanupEnabled: Boolean = true,
    val documentTtl: Duration = Duration.ofDays(DEFAULT_DOCUMENT_TTL_DAYS),
    val maxIndexAge: Duration = Duration.ofDays(DEFAULT_MAX_INDEX_AGE_DAYS),
    val maxSessions: Int = DEFAULT_MAX_SESSIONS,
) {
    init {
        require(!maxIndexAge.isNegative && !maxIndexAge.isZero) { "maxIndexAge must be positive" }
        require(maxSessions > 0) { "maxSessions must be greater than 0" }
        require(maxIndexAge < documentTtl) {
            "maxIndexAge ($maxIndexAge) must be shorter than documentTtl ($documentTtl), " +
                "otherwise the cleanup job can delete documents a running instance still considers indexed"
        }
    }

    companion object {
        private const val DEFAULT_DOCUMENT_TTL_DAYS = 30L
        private const val DEFAULT_MAX_INDEX_AGE_DAYS = 7L
        private const val DEFAULT_MAX_SESSIONS = 1_000
    }
}

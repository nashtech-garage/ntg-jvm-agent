package com.ntgjvmagent.orchestrator.entity.agent.knowledge

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "embedding_tier_config")
class EmbeddingTierConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    /**
     * Unique tier identifier such as "free", "pro", "enterprise".
     */
    @Column(name = "tier_name", unique = true, nullable = false)
    val tierName: String,
) {
    /**
     * Maximum number of API calls allowed during a single rate-limit interval.
     */
    @Column(name = "max_calls_per_interval", nullable = false)
    val maxCallsPerInterval: Int = 0

    /**
     * Length of the rate-limit interval in seconds.
     */
    @Column(name = "interval_seconds", nullable = false)
    val intervalSeconds: Long = 0

    /**
     * Maximum number of retry attempts.
     */
    @Column(name = "max_retries", nullable = false)
    val maxRetries: Int = 0

    /**
     * Initial delay (in ms) for exponential backoff retries.
     */
    @Column(name = "base_backoff_ms", nullable = false)
    val baseBackoffMs: Long = 0

    /**
     * Maximum allowed delay (in ms) for exponential backoff.
     */
    @Column(name = "max_backoff_ms", nullable = false)
    val maxBackoffMs: Long = 0
}

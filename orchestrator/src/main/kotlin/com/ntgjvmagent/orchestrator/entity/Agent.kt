package com.ntgjvmagent.orchestrator.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(
    name = "agent",
    indexes = [
        Index(name = "idx_agent_name", columnList = "name"),
        Index(name = "idx_agent_model", columnList = "model"),
        Index(name = "idx_agent_provider", columnList = "provider"),
        Index(name = "idx_agent_active", columnList = "active"),
        Index(name = "idx_agent_deleted_at", columnList = "deleted_at"),
    ],
)
data class Agent(
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(nullable = false, length = 100)
    var model: String,
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    /**
     * Controls randomness of model output.
     * 0.0 = deterministic, 2.0 = very creative.
     */
    @Column(nullable = false, precision = 3, scale = 2)
    var temperature: BigDecimal = DEFAULT_TEMPERATURE,
    /**
     * Maximum number of tokens the model may generate in a single response.
     * Depends on model capacity (GPT-4o supports up to 16k).
     */
    @Column(name = "max_tokens", nullable = false)
    var maxTokens: Int = DEFAULT_MAX_TOKENS,
    /**
     * Nucleus sampling probability cutoff.
     * 1.0 means all tokens are considered.
     */
    @Column(name = "top_p", nullable = false, precision = 3, scale = 2)
    var topP: BigDecimal = DEFAULT_TOP_P,
    /**
     * Penalizes new tokens based on their frequency in the text so far.
     * Range: -2.0 (encourage repetition) to 2.0 (strongly discourage repetition).
     */
    @Column(name = "frequency_penalty", nullable = false, precision = 3, scale = 2)
    var frequencyPenalty: BigDecimal = DEFAULT_FREQUENCY_PENALTY,
    /**
     * Penalizes new tokens if they already appear in the text.
     * Range: -2.0 (encourage reuse) to 2.0 (discourage reuse).
     */
    @Column(name = "presence_penalty", nullable = false, precision = 3, scale = 2)
    var presencePenalty: BigDecimal = DEFAULT_PRESENCE_PENALTY,
    @Column(nullable = false)
    var active: Boolean = true,
    @Column(length = 50)
    var provider: String? = null, // e.g. "openai", "anthropic", "local"
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var settings: Map<String, Any>? = null, // JSON for flexible configuration
    @Version
    @Column(nullable = false)
    var version: Int = 0,
    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = null,
) : BaseEntity() {
    companion object {
        /** Default OpenAI-compatible parameter values **/
        val DEFAULT_TEMPERATURE: BigDecimal = BigDecimal("0.7")
        const val DEFAULT_MAX_TOKENS: Int = 2_048
        val DEFAULT_TOP_P: BigDecimal = BigDecimal("1.0")
        val DEFAULT_FREQUENCY_PENALTY: BigDecimal = BigDecimal("0.0")
        val DEFAULT_PRESENCE_PENALTY: BigDecimal = BigDecimal("0.0")

        /** Parameter constraints for validation consistency **/
        const val MIN_MAX_TOKENS = 1
        const val MAX_MAX_TOKENS = 16_384
        const val MIN_TEMPERATURE = 0.0
        const val MAX_TEMPERATURE = 2.0
        const val MIN_TOP_P = 0.0
        const val MAX_TOP_P = 1.0
        const val MIN_PENALTY = -2.0
        const val MAX_PENALTY = 2.0
    }
}
